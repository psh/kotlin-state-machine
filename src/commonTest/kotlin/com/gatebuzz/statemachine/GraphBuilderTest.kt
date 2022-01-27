package com.gatebuzz.statemachine

import com.gatebuzz.statemachine.MachineState.Dwelling
import com.gatebuzz.statemachine.TestEvents.OtherTestEvent
import com.gatebuzz.statemachine.TestEvents.TestEvent
import com.gatebuzz.statemachine.TestState.*
import com.gatebuzz.verification.TestActionResult
import com.gatebuzz.verification.TestEdgeVisitor
import com.gatebuzz.verification.TestStateVisitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@ExperimentalCoroutinesApi
class GraphBuilderTest {

    private val enterA = TestStateVisitor()
    private val exitA = TestStateVisitor()
    private val enterB = TestStateVisitor()
    private val exitB = TestStateVisitor()

    private val enterEdgeAB: TestEdgeVisitor = TestEdgeVisitor()
    private val exitEdgeAB: TestEdgeVisitor = TestEdgeVisitor()

    private var edgeActionABCalled = false
    private var edgeActionBACalled = false
    private val edgeActionAB: EdgeAction = { edgeActionABCalled = true }
    private val edgeActionBA: EdgeAction = { edgeActionBACalled = true }
    private val actionResult: TestActionResult = TestActionResult()

    //region build a graph from nodes and events
    @Test
    fun createEmptyGraph() = runTest {
        val testObject = graph {}
        val expected = Graph()
        assertEquals(expected, testObject)
    }

    @Test
    fun addNode() = runTest {
        val testObject = graph {
            state(StateA)
        }

        val expected = Graph().apply {
            add(StateA)
        }
        assertEquals(expected, testObject)
    }

    @Test
    fun addMoreNodes() = runTest {
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
    fun availableEdgesWithoutEvents() = runTest {
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
    fun initialState() = runTest {
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
    fun addEvent() = runTest {
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
    fun transitionToNewState() = runTest {
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
    fun cannotTransitionToNodesOutsideOfTheGraph() = runTest {
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
    fun stateEntryActionsAreExecutedWhenStarting() = runTest {
        val testObject = graph {
            initialState(StateA)
            state(StateA) { onEnter(enterA::accept) }
            state(StateB)
        }

        testObject.start()

        assertTrue(enterA.wasCalled)
        assertEquals(StateA, enterA.state)
        assertEquals(null, enterA.trigger)
    }

    @Test
    fun stateEntryActionsAreExecutedWhenTraversing() = runTest {
        val testObject = graph {
            initialState(StateA)
            state(StateA) { onEnter(enterA::accept) }
            state(StateB) { onEnter(enterB::accept) }
        }
        testObject.start()

        testObject.transitionTo(StateB)

        assertTrue(enterA wasCalledBefore enterB)
        assertEquals(StateA, enterA.state)
        assertEquals(StateB, enterB.state)
    }

    @Test
    fun traversalOnEnterIsExecuted() = runTest {
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

        assertTrue(
            enterA wasCalledBefore enterEdgeAB &&
                    enterEdgeAB wasCalledBefore enterB
        )
        assertEquals(StateA, enterA.state)
        assertEquals(StateA to StateB, enterEdgeAB.edge)
        assertEquals(StateB, enterB.state)
    }

    @Test
    fun stateExitActionsAreExecutedWhenTraversing() = runTest {
        val testObject = graph {
            initialState(StateA)
            state(StateA) { onExit(exitA::accept) }
            state(StateB)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        assertTrue(exitA.wasCalled)
        assertEquals(StateA, exitA.state)
    }

    @Test
    fun traversalOnExitIsExecuted() = runTest {
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

        assertTrue(exitA wasCalledBefore exitEdgeAB)
        assertEquals(StateA, exitA.state)
        assertEquals(StateA to StateB, exitEdgeAB.edge)
    }

    @Test
    fun traversalEntryAndExitCreatedByEvent() = runTest {
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
        assertTrue(enterA.wasCalled)
        assertEquals(StateA, enterA.state)

        testObject.transitionTo(StateB)

        assertTrue(
            exitA wasCalledBefore enterEdgeAB &&
                    enterEdgeAB wasCalledBefore exitEdgeAB &&
                    exitEdgeAB wasCalledBefore enterB
        )
        assertEquals(StateA, exitA.state)
        assertEquals(StateA to StateB, enterEdgeAB.edge)
        assertEquals(StateA to StateB, exitEdgeAB.edge)
        assertEquals(StateB, enterB.state)
    }

    @Test
    fun traversalEntryAndExitCreatedByDefinedStateTransition() = runTest {
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
        assertTrue(enterA.wasCalled)
        assertEquals(StateA, enterA.state)

        testObject.transitionTo(StateB)

        assertTrue(
            exitA wasCalledBefore enterEdgeAB &&
                    enterEdgeAB wasCalledBefore exitEdgeAB &&
                    exitEdgeAB wasCalledBefore enterB
        )
        assertEquals(StateA, exitA.state)
        assertEquals(StateA to StateB, enterEdgeAB.edge)
        assertEquals(StateA to StateB, exitEdgeAB.edge)
        assertEquals(StateB, enterB.state)
    }

    @Test
    fun entryAndExitCreatedByAllowedTransition() = runTest {
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
        assertTrue(enterA.wasCalled)
        assertEquals(StateA, enterA.state)

        testObject.transitionTo(StateB)

        assertTrue(exitA wasCalledBefore enterB)
        assertEquals(StateA, exitA.state)
        assertEquals(StateB, enterB.state)
    }
    //endregion

    //region traversal actions
    @Test
    fun traversalActionIsExecutedWhenEdgeIsDefinedByEvent() = runTest {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                on(TestEvent) {
                    transitionTo(StateB) { edgeActionAB.invoke(actionResult, it) }
                }
            }
            state(StateB)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        assertTrue(edgeActionABCalled)
    }

    @Test
    fun traversalActionIsExecutedLongForm() = runTest {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                on(TestEvent) {
                    transitionTo(StateB)
                    execute { edgeActionAB.invoke(actionResult, it) }
                }
            }
            state(StateB)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        assertTrue(edgeActionABCalled)
    }

    @Test
    fun traversalActionIsExecutedWhenEdgeIsDefinedByState() = runTest {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                onTransitionTo(StateB) {
                    execute { edgeActionAB.invoke(actionResult, it) }
                }
            }
            state(StateB)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        assertTrue(edgeActionABCalled)
    }

    @Test
    fun traversalSucceedsByDefault() = runTest {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                onEnter(enterA::accept)
                onExit(exitA::accept)
                on(TestEvent) {
                    onEnter(enterEdgeAB::accept)
                    onExit(exitEdgeAB::accept)
                    transitionTo(StateB) { }
                }
            }
            state(StateB) {
                onEnter(enterB::accept)
                onExit(exitB::accept)
            }
        }
        testObject.start()
        assertTrue(enterA.wasCalled)
        assertEquals(StateA, enterA.state)

        testObject.transitionTo(StateB)

        assertTrue(
            exitA wasCalledBefore enterEdgeAB &&
                    enterEdgeAB wasCalledBefore exitEdgeAB &&
                    exitEdgeAB wasCalledBefore enterB
        )

        assertEquals(StateA, exitA.state)
        assertEquals(StateA to StateB, enterEdgeAB.edge)
        assertEquals(StateA to StateB, exitEdgeAB.edge)
        assertEquals(StateB, enterB.state)
    }

    @Test
    fun traversalFails() = runTest {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                onEnter(enterA::accept)
                onExit(exitA::accept)
                on(TestEvent) {
                    onEnter(enterEdgeAB::accept)
                    onExit(exitEdgeAB::accept)
                    transitionTo(StateB) { fail() }
                }
            }
            state(StateB) {
                onEnter(enterB::accept)
                onExit(exitB::accept)
            }
        }
        testObject.start()
        assertTrue(enterA.wasCalled)
        assertEquals(StateA, enterA.state)

        testObject.transitionTo(StateB)

        assertTrue(exitA wasCalledBefore enterEdgeAB)
        assertEquals(StateA, exitA.state)
        assertEquals(StateA to StateB, enterEdgeAB.edge)
        assertTrue(exitEdgeAB.wasNotCalled)
        assertTrue(enterB.wasNotCalled)
    }

    @Test
    fun traversalFailButExitStillDesired() = runTest {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                onEnter(enterA::accept)
                onExit(exitA::accept)
                on(TestEvent) {
                    onEnter(enterEdgeAB::accept)
                    onExit(exitEdgeAB::accept)
                    transitionTo(StateB) { failAndExit() }
                }
            }
            state(StateB) {
                onEnter(enterB::accept)
                onExit(exitB::accept)
            }
        }
        testObject.start()
        assertTrue(enterA.wasCalled)
        assertEquals(StateA, enterA.state)

        testObject.transitionTo(StateB)

        assertTrue(
            exitA wasCalledBefore enterEdgeAB && enterEdgeAB wasCalledBefore exitEdgeAB
        )
        assertEquals(StateA, exitA.state)
        assertEquals(StateA to StateB, enterEdgeAB.edge)
        assertEquals(StateA to StateB, exitEdgeAB.edge)
        assertTrue(enterB.wasNotCalled)
    }
    //endregion

    //region event driven transition
    @Test
    fun traversalActionIsExecutedOnEvent() = runTest {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                on(TestEvent) {
                    transitionTo(StateB) { edgeActionAB.invoke(actionResult, it) }
                }
            }
            state(StateB)
        }
        testObject.start()

        testObject.consume(TestEvent)

        assertTrue(edgeActionABCalled)
    }

    @Test
    fun eventIsIgnoredIfWeAreInTheWrongState() = runTest {
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                on(TestEvent) {
                    transitionTo(StateB) { edgeActionAB.invoke(actionResult, it) }
                }
            }
            state(StateB) {
                on(OtherTestEvent) {
                    transitionTo(StateA) { edgeActionBA.invoke(actionResult, it) }
                }
            }
        }
        testObject.start()

        testObject.consume(OtherTestEvent)

        assertFalse(edgeActionABCalled)
        assertFalse(edgeActionBACalled)
    }
    //endregion

    //region decision states
    @Test
    fun decisionsProduceEventsCausingTransitions() = runTest {
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
    fun nullResultFromADecisionCausesNoTransition() = runTest {
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
    fun unhandledEventFromADecisionCausesNoTransition() = runTest {
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
