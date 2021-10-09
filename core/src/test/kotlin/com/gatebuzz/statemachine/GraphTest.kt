package com.gatebuzz.statemachine

import app.cash.turbine.test
import com.gatebuzz.statemachine.TestEvents.OtherTestEvent
import com.gatebuzz.statemachine.TestState.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
class GraphTest {

    private val nodeA = Node(StateA).apply {
        onEnter = mockk(relaxed = true)
        onExit = mockk(relaxed = true)
    }
    private val nodeB = Node(StateB).apply {
        onEnter = mockk(relaxed = true)
        onExit = mockk(relaxed = true)
    }
    private val nodeC = Node(StateC).apply {
        onEnter = mockk(relaxed = true)
        onExit = mockk(relaxed = true)
    }
    private val edgeAB = Edge(nodeA, nodeB).apply {
        onEnter = mockk(relaxed = true)
        onExit = mockk(relaxed = true)
    }
    private val edgeBA = Edge(nodeB, nodeA).apply {
        onEnter = mockk(relaxed = true)
        onExit = mockk(relaxed = true)
    }

    //region finding nodes, equality and hashcode
    @Test
    fun `graphs are equal when empty`() {
        assertTrue(Graph() == Graph())
    }

    @Test
    fun `graphs are equal with nodes`() {
        val testObject1 = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }
        val testObject2 = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }
        assertTrue(testObject1 == testObject2)
        assertEquals(testObject1.hashCode(), testObject2.hashCode())
    }

    @Test
    fun `find a node`() {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        assertEquals(nodeA, testObject.findNode(StateA))
        assertEquals(nodeB, testObject.findNode(StateB))
        assertNull(testObject.findNode(StateC))
    }
    //endregion

    //region starting the state machine
    @Test
    fun `empty graph starts inactive`() {
        val testObject = Graph()
        assertEquals(MachineState.Inactive(), testObject.initialState)
        assertEquals(MachineState.Inactive(), testObject.currentState)
    }

    @Test
    fun `graph with one node is inactive until started - observing state`() = runBlocking {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
        }
        assertEquals(MachineState.Inactive(), testObject.currentState)

        testObject.observeState().test {
            testObject.start()

            assertEquals(MachineState.Dwelling(StateA), testObject.currentState)
            assertEquals(StateA, awaitItem())
        }
    }

    @Test
    fun `graph with one node is inactive until started - observing machine state`() = runBlocking {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
        }
        assertEquals(MachineState.Inactive(), testObject.currentState)

        testObject.observeStateChanges().test {
            testObject.start()

            assertEquals(MachineState.Dwelling(StateA), testObject.currentState)
            assertEquals(MachineState.Dwelling(StateA), awaitItem())
        }
    }
    //endregion

    //region simple state transition
    @Test
    fun `transition to new state`() {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        testObject.start()

        val newNode = testObject.transitionTo(StateB)

        assertEquals(StateB, newNode)
        assertEquals(MachineState.Dwelling(StateB), testObject.currentState)
    }

    @Test
    fun `transition to new state via state id`() {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        testObject.start()

        val newNode = testObject.transitionTo(StateB)

        assertEquals(StateB, newNode)
        assertEquals(MachineState.Dwelling(StateB), testObject.currentState)
    }

    @Test
    fun `cannot transition to nodes outside of the graph`() = runBlocking {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        testObject.start()

        testObject.observeState().test {
            val newNode = testObject.transitionTo(StateC)
            assertNull(newNode)
            assertEquals(MachineState.Dwelling(StateA), testObject.currentState)
            expectNoEvents()
        }
    }

    @Test
    fun `observe state changes`() = runBlocking {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }

        testObject.observeState().test {
            testObject.start()
            assertEquals(StateA, awaitItem())

            testObject.transitionTo(StateB)
            assertEquals(StateB, awaitItem())
        }
    }

    @Test
    fun `observed machine state change includes traversal`() = runBlocking {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }

        testObject.observeStateChanges().test {
            testObject.start()
            assertEquals(MachineState.Dwelling(StateA), awaitItem())

            testObject.transitionTo(StateB)
            assertEquals(MachineState.Traversing(edge = Edge(StateA to StateB), trigger = null), awaitItem())
            assertEquals(MachineState.Dwelling(StateB), awaitItem())
        }
    }
    //endregion

    //region entry & exit actions
    @Test
    fun `state entry actions are executed when starting`() {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        testObject.start()

        verify { nodeA.onEnter.accept(StateA, null) }
    }

    @Test
    fun `state entry actions are executed when traversing`() {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        verifyOrder {
            nodeA.onEnter.accept(StateA, null)
            nodeB.onEnter.accept(StateB, null)
        }
    }

    @Test
    fun `traversal on enter is executed`() {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        verifyOrder {
            nodeA.onEnter.accept(StateA, null)
            edgeAB.onEnter.accept(StateA to StateB)
            nodeB.onEnter.accept(StateB, null)
        }
    }

    @Test
    fun `state exit actions are executed when traversing`() {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        verify { nodeA.onExit.accept(StateA, null) }
    }

    @Test
    fun `traversal on exit is executed`() {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        verifyOrder {
            nodeA.onExit.accept(StateA, null)
            edgeAB.onExit.accept(StateA to StateB)
        }
    }

    @Test
    fun `traversal entry and exit`() {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }
        testObject.start()
        verify { nodeA.onEnter.accept(StateA, null) }

        testObject.transitionTo(StateB)

        verifyOrder {
            nodeA.onExit.accept(StateA, null)
            edgeAB.onEnter.accept(StateA to StateB)
            edgeAB.onExit.accept(StateA to StateB)
            nodeB.onEnter.accept(StateB, null)
        }
    }
    //endregion

    //region traversal actions
    @Test
    fun `traversal action is executed`() {
        edgeAB.action = mockk(relaxed = true)
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        verify { edgeAB.action.execute(any(), ofType()) }
    }

    @Test
    fun `traversal succeeds`() {
        edgeAB.action = EdgeAction { tr, result -> result.success(tr) }
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }

        testObject.start()
        testObject.transitionTo(StateB)

        verifyOrder {
            nodeA.onExit.accept(StateA, null)

            edgeAB.onEnter.accept(StateA to StateB)
            edgeAB.onExit.accept(StateA to StateB)

            nodeB.onEnter.accept(StateB, null)
        }
    }

    @Test
    fun `traversal fails`() {
        edgeAB.action = EdgeAction { tr, result -> result.failure(tr) }
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }

        testObject.start()
        testObject.transitionTo(StateB)

        verifyOrder {
            nodeA.onEnter.accept(StateA, null)
            nodeA.onExit.accept(StateA, null)

            edgeAB.onEnter.accept(StateA to StateB)
            nodeA.onEnter.accept(StateA, null)
        }
        verify { listOf(nodeB.onEnter, edgeAB.onExit) wasNot Called }
    }
    //endregion

    //region event driven transition
    @Test
    fun `traversal action is executed on event`() {
        edgeAB.action = mockk(relaxed = true)
        val event: Event = mockk(relaxed = true)
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
            addEvent(event, edgeAB)
        }
        testObject.start()

        testObject.consume(event)

        verify { edgeAB.action.execute(any(), ofType()) }
    }

    @Test
    fun `event is ignored if we are in the wrong state`() {
        edgeAB.action = mockk(relaxed = true)
        edgeBA.action = mockk(relaxed = true)
        val event: Event = mockk(relaxed = true)
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
            add(edgeBA)
            addEvent(event, edgeBA)
        }
        testObject.start()

        testObject.consume(event)

        verify { listOf(edgeAB.action, edgeBA.action) wasNot Called }
    }

    @Test
    fun `observed machine state change includes traversal when consuming event`() = runBlocking {
        val event: Event = mockk(relaxed = true)
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
            addEvent(event, edgeAB)
        }

        testObject.observeStateChanges().test {
            testObject.start()
            assertEquals(MachineState.Dwelling(StateA), awaitItem())

            testObject.consume(event)
            assertEquals(MachineState.Traversing(edge = Edge(StateA to StateB), trigger = event), awaitItem())
            assertEquals(MachineState.Dwelling(StateB), awaitItem())
        }
    }
    //endregion

    //region decision states
    @Test
    fun `decisions take precedence over onEnter`() {
        nodeB.decision = mockk(relaxed = true)
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        verifyOrder {
            nodeA.onEnter.accept(StateA, null)
            nodeB.decision!!.decide(StateB, null)
        }
        verify(exactly = 0) { nodeB.onEnter.accept(any(), isNull()) }
    }

    @Test
    fun `decisions trigger events`() {
        nodeB.decision = mockk(relaxed = true)
        every { nodeB.decision!!.decide(any(), any()) } returns OtherTestEvent

        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(nodeC)
            addEvent(OtherTestEvent, Edge(nodeB, nodeC))
        }
        testObject.start()

        testObject.transitionTo(StateB)

        verifyOrder {
            nodeA.onEnter.accept(StateA, null)
            nodeB.decision!!.decide(StateB, null)
            nodeB.onExit.accept(StateB, OtherTestEvent)
            nodeC.onEnter.accept(StateC, OtherTestEvent)
        }
        verify(exactly = 0) { nodeB.onEnter.accept(any(), any()) }
    }

    @Test
    fun `null decisions have no effect`() {
        nodeB.decision = mockk(relaxed = true)
        every { nodeB.decision!!.decide(any(), any()) } returns null

        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(nodeC)
            addEvent(OtherTestEvent, Edge(StateB to StateC))
        }
        testObject.start()

        testObject.transitionTo(StateB)

        verifyOrder {
            nodeA.onEnter.accept(StateA, null)
            nodeB.decision!!.decide(StateB, null)
        }
        verify(exactly = 0) { nodeB.onEnter.accept(any(), any()) }
        verify(exactly = 0) { nodeB.onExit.accept(StateB, null) }
        verify(exactly = 0) { nodeC.onEnter.accept(StateC, null) }
    }

    //endregion
}
