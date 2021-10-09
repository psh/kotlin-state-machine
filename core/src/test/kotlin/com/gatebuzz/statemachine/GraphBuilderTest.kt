package com.gatebuzz.statemachine

import com.gatebuzz.statemachine.MachineState.Dwelling
import com.gatebuzz.statemachine.TestEvents.OtherTestEvent
import com.gatebuzz.statemachine.TestEvents.TestEvent
import com.gatebuzz.statemachine.TestState.*
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GraphBuilderTest {

    private val enterA: StateVisitor = mockk(relaxed = true)
    private val exitA: StateVisitor = mockk(relaxed = true)
    private val enterB: StateVisitor = mockk(relaxed = true)
    private val exitB: StateVisitor = mockk(relaxed = true)

    private val enterEdgeAB: EdgeVisitor = mockk(relaxed = true)
    private val exitEdgeAB: EdgeVisitor = mockk(relaxed = true)
    private val edgeActionAB: EdgeAction = mockk(relaxed = true)
    private val edgeActionBA: EdgeAction = mockk(relaxed = true)

    //region build a graph from nodes and events
    @Test
    fun `create empty graph`() {
        val testObject = graph {}
        val expected = Graph()
        assertEquals(expected, testObject)
    }

    @Test
    fun `add a node`() {
        val testObject = graph {
            state(StateA)
        }

        val expected = Graph().apply {
            add(StateA)
        }
        assertEquals(expected, testObject)
    }

    @Test
    fun `add more nodes`() {
        val testObject = graph {
            state(StateA)
            state(StateB)
            state(StateC)
        }

        val expected = Graph().apply {
            add(StateA)
            add(StateB)
            add(StateC)
        }
        assertEquals(expected, testObject)
    }

    @Test
    fun `available edges without events`() {
        val testObject = graph {
            state(StateA) {
                allows(StateB)
            }
            state(StateB) {
                allows(StateA, StateC)
            }
            state(StateC) {
                allows(StateB)
            }
        }

        val expected = Graph().apply {
            add(StateA)
            add(StateB)
            add(StateC)

            add(StateA to StateB)
            add(StateB to StateA)
            add(StateB to StateC)
            add(StateC to StateB)
        }
        assertEquals(expected, testObject)
    }

    @Test
    fun `initial state`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA)
        }

        val expected = Graph().apply {
            initialState = Dwelling(StateA)
            add(StateA)
        }
        assertEquals(expected, testObject)
    }

    @Test
    fun `add event`() {
        val testObject = graph {
            initialState(StateA)
            state(StateB)
            state(StateA) {
                on(TestEvent) { transitionTo(StateB) }
            }
        }

        val expected = Graph().apply {
            initialState = Dwelling(StateA)
            add(StateA)
            add(StateB)
            add(StateA to StateB)
            addEvent(TestEvent, Edge(StateA to StateB))
        }

        testObject.start()
        expected.start()

        assertEquals(expected, testObject)
    }
    //endregion

    //region simple state transition
    @Test
    fun `transition to new state`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA)
            state(StateB)
        }
        testObject.start()

        val newState = testObject.transitionTo(StateB)

        assertEquals(StateB, newState)
        assertEquals(Dwelling(StateB), testObject.currentState)
    }

    @Test
    fun `cannot transition to nodes outside of the graph`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA)
            state(StateB)
        }
        testObject.start()

        val newNode = testObject.transitionTo(StateC)

        assertNull(newNode)
        assertEquals(Dwelling(StateA), testObject.currentState)
    }
    //endregion

    //region entry & exit actions
    @Test
    fun `state entry actions are executed when starting`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA) { onEnter(enterA::accept) }
            state(StateB)
        }

        testObject.start()

        verify { enterA.accept(StateA, null) }
    }

    @Test
    fun `state entry actions are executed when traversing`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA) { onEnter(enterA::accept) }
            state(StateB) { onEnter(enterB::accept) }
        }
        testObject.start()

        testObject.transitionTo(StateB)

        verifyOrder {
            enterA.accept(StateA, null)
            enterB.accept(StateB, null)
        }
    }

    @Test
    fun `traversal on enter is executed`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                onEnter(enterA::accept)
                on(TestEvent) {
                    onEnter(enterEdgeAB::accept)
                    transitionTo(StateB)
                }
            }
            state(StateB) { onEnter(enterB::accept) }
        }
        testObject.start()

        testObject.transitionTo(StateB)

        verifyOrder {
            enterA.accept(StateA, null)
            enterEdgeAB.accept(StateA to StateB)
            enterB.accept(StateB, null)
        }
    }

    @Test
    fun `state exit actions are executed when traversing`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA) { onExit(exitA::accept) }
            state(StateB)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        verify { exitA.accept(StateA, null) }
    }

    @Test
    fun `traversal on exit is executed`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                onExit(exitA::accept)
                on(TestEvent) {
                    onExit(exitEdgeAB::accept)
                    transitionTo(StateB)
                }
            }
            state(StateB) { onExit(exitB::accept) }
        }
        testObject.start()

        testObject.transitionTo(StateB)

        verifyOrder {
            exitA.accept(StateA, null)
            exitEdgeAB.accept(StateA to StateB)
        }
    }

    @Test
    fun `traversal entry and exit created by event`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                onEnter(enterA::accept)
                onExit(exitA::accept)
                on(TestEvent) {
                    onEnter(enterEdgeAB::accept)
                    onExit(exitEdgeAB::accept)
                    transitionTo(StateB)
                }
            }
            state(StateB) {
                onEnter(enterB::accept)
                onExit(exitB::accept)
            }
        }
        testObject.start()
        verify { enterA.accept(StateA, null) }

        testObject.transitionTo(StateB)

        verifyOrder {
            exitA.accept(StateA, null)
            enterEdgeAB.accept(StateA to StateB)
            exitEdgeAB.accept(StateA to StateB)
            enterB.accept(StateB, null)
        }
    }

    @Test
    fun `traversal entry and exit created by defined state transition`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                onEnter(enterA::accept)
                onExit(exitA::accept)
                onTransitionTo(StateB) {
                    onEnter(enterEdgeAB::accept)
                    onExit(exitEdgeAB::accept)
                    transitionTo(StateB)
                }
            }
            state(StateB) {
                onEnter(enterB::accept)
                onExit(exitB::accept)
            }
        }
        testObject.start()
        verify { enterA.accept(StateA, null) }

        testObject.transitionTo(StateB)

        verifyOrder {
            exitA.accept(StateA, null)
            enterEdgeAB.accept(StateA to StateB)
            exitEdgeAB.accept(StateA to StateB)
            enterB.accept(StateB, null)
        }
    }

    @Test
    fun `entry and exit created by allowed transition`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                onEnter(enterA::accept)
                onExit(exitA::accept)
                allows(StateB)
            }
            state(StateB) {
                onEnter(enterB::accept)
                onExit(exitB::accept)
            }
        }
        testObject.start()
        verify { enterA.accept(StateA, null) }

        testObject.transitionTo(StateB)

        verifyOrder {
            exitA.accept(StateA, null)
            enterB.accept(StateB, null)
        }
    }
    //endregion

    //region traversal actions
    @Test
    fun `traversal action is executed when edge is defined by event`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                on(TestEvent) {
                    transitionTo(StateB) { trigger, result ->
                        edgeActionAB.execute(trigger, result)
                        result.success(trigger)
                    }
                }
            }
            state(StateB)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        verify { edgeActionAB.execute(any(), ofType()) }
    }

    @Test
    fun `traversal action is executed - long form`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                on(TestEvent) {
                    transitionTo(StateB)
                    execute { trigger, result ->
                        edgeActionAB.execute(trigger, result)
                        result.success(trigger)
                    }
                }
            }
            state(StateB)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        verify { edgeActionAB.execute(any(), ofType()) }
    }

    @Test
    fun `traversal action is executed when edge is defined by state`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                onTransitionTo(StateB) {
                    execute { trigger, result ->
                        edgeActionAB.execute(trigger, result)
                        result.success(trigger)
                    }
                }
            }
            state(StateB)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        verify { edgeActionAB.execute(any(), ofType()) }
    }

    @Test
    fun `traversal succeeds`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                onEnter(enterA::accept)
                onExit(exitA::accept)
                on(TestEvent) {
                    onEnter(enterEdgeAB::accept)
                    onExit(exitEdgeAB::accept)
                    transitionTo(StateB) { trigger, result ->
                        result.success(trigger)
                    }
                }
            }
            state(StateB) {
                onEnter(enterB::accept)
                onExit(exitB::accept)
            }
        }
        testObject.start()
        verify { enterA.accept(StateA, null) }

        testObject.transitionTo(StateB)

        verifyOrder {
            exitA.accept(StateA, null)
            enterEdgeAB.accept(StateA to StateB)
            exitEdgeAB.accept(StateA to StateB)
            enterB.accept(StateB, null)
        }
    }

    @Test
    fun `traversal fails`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                onEnter(enterA::accept)
                onExit(exitA::accept)
                on(TestEvent) {
                    onEnter(enterEdgeAB::accept)
                    onExit(exitEdgeAB::accept)
                    transitionTo(StateB) { trigger, result ->
                        result.failure(trigger)
                    }
                }
            }
            state(StateB) {
                onEnter(enterB::accept)
                onExit(exitB::accept)
            }
        }
        testObject.start()
        verify { enterA.accept(StateA, null) }

        testObject.transitionTo(StateB)

        verifyOrder {
            exitA.accept(StateA, null)
            enterEdgeAB.accept(StateA to StateB)
        }
        verify(exactly = 0) { exitEdgeAB.accept(StateA to StateB) }
        verify(exactly = 0) { enterB.accept(StateB, null) }
    }

    @Test
    fun `traversal fail but exit still desired`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                onEnter(enterA::accept)
                onExit(exitA::accept)
                on(TestEvent) {
                    onEnter(enterEdgeAB::accept)
                    onExit(exitEdgeAB::accept)
                    transitionTo(StateB) { trigger, result ->
                        result.failAndExit(trigger)
                    }
                }
            }
            state(StateB) {
                onEnter(enterB::accept)
                onExit(exitB::accept)
            }
        }
        testObject.start()
        verify { enterA.accept(StateA, null) }

        testObject.transitionTo(StateB)

        verifyOrder {
            exitA.accept(StateA, null)
            enterEdgeAB.accept(StateA to StateB)
            exitEdgeAB.accept(StateA to StateB)
        }
        verify(exactly = 0) { enterB.accept(StateB, null) }
    }
    //endregion

    //region event driven transition
    @Test
    fun `traversal action is executed on event`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                on(TestEvent) {
                    transitionTo(StateB) { trigger, result ->
                        edgeActionAB.execute(trigger, result)
                        result.success(trigger)
                    }
                }
            }
            state(StateB)
        }
        testObject.start()

        testObject.consume(TestEvent)

        verify { edgeActionAB.execute(any(), ofType()) }
    }

    @Test
    fun `event is ignored if we are in the wrong state`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                on(TestEvent) {
                    transitionTo(StateB) { trigger, result ->
                        edgeActionAB.execute(trigger, result)
                        result.success(trigger)
                    }
                }
            }
            state(StateB) {
                on(OtherTestEvent) {
                    transitionTo(StateA) { trigger, result ->
                        edgeActionBA.execute(trigger, result)
                        result.success(trigger)
                    }
                }
            }
        }
        testObject.start()

        testObject.consume(OtherTestEvent)

        verify { listOf(edgeActionAB, edgeActionBA) wasNot Called }
    }
    //endregion

    //region decision states
    @Test
    fun `decisions produce events causing transitions`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                allows(StateB)
            }
            state(StateB) {
                decision { _, _ -> OtherTestEvent }
                on(TestEvent) { transitionTo(StateA) }
                on(OtherTestEvent) { transitionTo(StateC) }
            }
            state(StateC)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        assertEquals(StateC, testObject.currentState.id)
    }

    @Test
    fun `null result from a decision causes no transition`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                allows(StateB)
            }
            state(StateB) {
                decision { _, _ -> null }
                on(TestEvent) { transitionTo(StateA) }
                on(OtherTestEvent) { transitionTo(StateC) }
            }
            state(StateC)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        assertEquals(StateB, testObject.currentState.id)
    }

    @Test
    fun `unhandled event from a decision causes no transition`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                allows(StateB)
            }
            state(StateB) {
                decision { _, _ -> TestEvent }
                on(OtherTestEvent) { transitionTo(StateC) }
            }
            state(StateC)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        assertEquals(StateB, testObject.currentState.id)
    }
    //endregion
}
