package com.gatebuzz.statemachine

import com.gatebuzz.statemachine.MachineState.Dwelling
import com.gatebuzz.statemachine.TestEvents.OtherTestEvent
import com.gatebuzz.statemachine.TestEvents.TestEvent
import com.gatebuzz.statemachine.TestState.*
import com.nhaarman.mockitokotlin2.*
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class GraphBuilderTest {

    private val transitionListener: StateTransitionListener = mock()
    private val stateListener: StateListener = mock()
    private val nodeA = Node(StateA)
    private val nodeB = Node(StateB)
    private val nodeC = Node(StateC)
    private val edgeAB = Edge(nodeA, nodeB)

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
            add(Node(StateA))
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
            add(Node(StateA))
            add(Node(StateB))
            add(Node(StateC))
        }
        assertEquals(expected, testObject)
    }

    @Test
    fun `available edges without events`(){
        val testObject = graph {
            state(StateA) {
                allows(StateB)
            }
            state(StateB){
                allows(StateA, StateC)
            }
            state(StateC) {
                allows(StateB)
            }
        }

        val expected = Graph().apply {
            add(Node(StateA))
            add(Node(StateB))
            add(Node(StateC))

            add(Edge(Node(StateA), Node(StateB)))
            add(Edge(Node(StateB), Node(StateA)))
            add(Edge(Node(StateB), Node(StateC)))
            add(Edge(Node(StateC), Node(StateB)))
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
            initialState = Dwelling(Node(StateA))
            add(Node(StateA))
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
            initialState = Dwelling(Node(StateA))
            add(Node(StateA))
            add(Node(StateB))
            add(Edge(Node(StateA), Node(StateB)))
            addEvent(TestEvent, Edge(Node(StateA), Node(StateB)))
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

        val newNode = testObject.transitionTo(nodeB)

        assertEquals(nodeB, newNode)
        assertEquals(Dwelling(nodeB), testObject.currentState)
    }

    @Test
    fun `cannot transition to nodes outside of the graph`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA)
            state(StateB)
        }
        testObject.start()
        testObject.addStateChangeListener(transitionListener)
        testObject.addStateListener(stateListener)

        val newNode = testObject.transitionTo(nodeC)

        Assert.assertNull(newNode)
        assertEquals(Dwelling(nodeA), testObject.currentState)
        verifyZeroInteractions(transitionListener, stateListener)
    }

    @Test
    fun `state change includes traversal`() {
        val testObject = graph {
            initialState(StateA)
            state(StateA)
            state(StateB)
            onTransition(transitionListener::onStateTransition)
            onState(stateListener::onState)
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        inOrder(transitionListener) {
            verify(transitionListener).onStateTransition(Dwelling(nodeA))
            verify(transitionListener).onStateTransition(MachineState.Traversing(Edge(nodeA, nodeB)))
            verify(transitionListener).onStateTransition(Dwelling(nodeB))
        }
        inOrder(stateListener) {
            verify(stateListener).onState(StateA)
            verify(stateListener).onState(StateB)
        }
    }
    //endregion

    //region entry & exit actions
    @Test
    fun `state entry actions are executed when starting`() {
        val onEnter: NodeVisitor = mock()
        val testObject = graph {
            initialState(StateA)
            state(StateA) { onEnter(onEnter::accept) }
            state(StateB)
        }

        testObject.start()

        verify(onEnter).accept(nodeA)
    }

    @Test
    fun `state entry actions are executed when traversing`() {
        val onEnterA: NodeVisitor = mock()
        val onEnterB: NodeVisitor = mock()
        val testObject = graph {
            initialState(StateA)
            state(StateA) { onEnter(onEnterA::accept) }
            state(StateB) { onEnter(onEnterB::accept) }
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        inOrder(onEnterA, onEnterB) {
            verify(onEnterA).accept(nodeA)
            verify(onEnterB).accept(nodeB)
        }
    }

    @Test
    fun `traversal on enter is executed`() {
        val onEnterA: NodeVisitor = mock()
        val onEnterB: NodeVisitor = mock()
        val onEnterEdgeAB: EdgeVisitor = mock()
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                onEnter(onEnterA::accept)
                on(TestEvent) {
                    onEnter(onEnterEdgeAB::accept)
                    transitionTo(StateB)
                }
            }
            state(StateB) { onEnter(onEnterB::accept) }
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        inOrder(onEnterA, onEnterB, onEnterEdgeAB) {
            verify(onEnterA).accept(nodeA)
            verify(onEnterEdgeAB).accept(edgeAB)
            verify(onEnterB).accept(nodeB)
        }
    }

    @Test
    fun `state exit actions are executed when traversing`() {
        val onExit: NodeVisitor = mock()
        val testObject = graph {
            initialState(StateA)
            state(StateA) { onExit(onExit::accept) }
            state(StateB)
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        verify(onExit).accept(nodeA)
    }

    @Test
    fun `traversal on exit is executed`() {
        val exitA: NodeVisitor = mock()
        val exitB: NodeVisitor = mock()
        val exitEdgeAB: EdgeVisitor = mock()
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

        testObject.transitionTo(nodeB)

        inOrder(exitA, exitB, exitEdgeAB) {
            verify(exitA).accept(nodeA)
            verify(exitEdgeAB).accept(edgeAB)
        }
    }

    @Test
    fun `traversal entry and exit created by event`() {
        val enterA: NodeVisitor = mock()
        val exitA: NodeVisitor = mock()
        val enterB: NodeVisitor = mock()
        val exitB: NodeVisitor = mock()
        val enterEdgeAB: EdgeVisitor = mock()
        val exitEdgeAB: EdgeVisitor = mock()
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
        verify(enterA).accept(nodeA)

        testObject.transitionTo(nodeB)

        inOrder(enterB, enterEdgeAB, exitA, exitEdgeAB) {
            verify(exitA).accept(nodeA)
            verify(enterEdgeAB).accept(edgeAB)
            verify(exitEdgeAB).accept(edgeAB)
            verify(enterB).accept(nodeB)
        }
    }

    @Test
    fun `traversal entry and exit created by defined state transition`() {
        val enterA: NodeVisitor = mock()
        val exitA: NodeVisitor = mock()
        val enterB: NodeVisitor = mock()
        val exitB: NodeVisitor = mock()
        val enterEdgeAB: EdgeVisitor = mock()
        val exitEdgeAB: EdgeVisitor = mock()
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
        verify(enterA).accept(nodeA)

        testObject.transitionTo(nodeB)

        inOrder(enterB, enterEdgeAB, exitA, exitEdgeAB) {
            verify(exitA).accept(nodeA)
            verify(enterEdgeAB).accept(edgeAB)
            verify(exitEdgeAB).accept(edgeAB)
            verify(enterB).accept(nodeB)
        }
    }

    @Test
    fun `entry and exit created by allowed transition`() {
        val enterA: NodeVisitor = mock()
        val exitA: NodeVisitor = mock()
        val enterB: NodeVisitor = mock()
        val exitB: NodeVisitor = mock()
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
        verify(enterA).accept(nodeA)

        testObject.transitionTo(nodeB)

        inOrder(enterB, exitA) {
            verify(exitA).accept(nodeA)
            verify(enterB).accept(nodeB)
        }
    }
    //endregion

    //region traversal actions
    @Test
    fun `traversal action is executed when edge is defined by event`() {
        val edgeAction: EdgeAction = mock()
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                on(TestEvent) {
                    transitionTo(StateB) {
                        edgeAction.execute(it)
                        it.success()
                    }
                }
            }
            state(StateB)
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        verify(edgeAction).execute(isA())
    }

    @Test
    fun `traversal action is executed - long form`() {
        val edgeAction: EdgeAction = mock()
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                on(TestEvent) {
                    transitionTo(StateB)
                    execute {
                        edgeAction.execute(it)
                        it.success()
                    }
                }
            }
            state(StateB)
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        verify(edgeAction).execute(isA())
    }

    @Test
    fun `traversal action is executed when edge is defined by state`() {
        val edgeAction: EdgeAction = mock()
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                onTransitionTo(StateB) {
                    execute {
                        edgeAction.execute(it)
                        it.success()
                    }
                }
            }
            state(StateB)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        verify(edgeAction).execute(isA())
    }

    @Test
    fun `traversal succeeds`() {
        val enterA: NodeVisitor = mock()
        val exitA: NodeVisitor = mock()
        val enterB: NodeVisitor = mock()
        val exitB: NodeVisitor = mock()
        val enterEdgeAB: EdgeVisitor = mock()
        val exitEdgeAB: EdgeVisitor = mock()
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                onEnter(enterA::accept)
                onExit(exitA::accept)
                on(TestEvent) {
                    onEnter(enterEdgeAB::accept)
                    onExit(exitEdgeAB::accept)
                    transitionTo(StateB) {
                        it.success()
                    }
                }
            }
            state(StateB) {
                onEnter(enterB::accept)
                onExit(exitB::accept)
            }
        }
        testObject.start()
        verify(enterA).accept(nodeA)

        testObject.transitionTo(nodeB)

        inOrder(enterB, enterEdgeAB, exitA, exitEdgeAB) {
            verify(exitA).accept(nodeA)
            verify(enterEdgeAB).accept(edgeAB)
            verify(exitEdgeAB).accept(edgeAB)
            verify(enterB).accept(nodeB)
        }
    }

    @Test
    fun `traversal fails`() {
        val enterA: NodeVisitor = mock()
        val exitA: NodeVisitor = mock()
        val enterB: NodeVisitor = mock()
        val exitB: NodeVisitor = mock()
        val enterEdgeAB: EdgeVisitor = mock()
        val exitEdgeAB: EdgeVisitor = mock()
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                onEnter(enterA::accept)
                onExit(exitA::accept)
                on(TestEvent) {
                    onEnter(enterEdgeAB::accept)
                    onExit(exitEdgeAB::accept)
                    transitionTo(StateB) {
                        it.failure()
                    }
                }
            }
            state(StateB) {
                onEnter(enterB::accept)
                onExit(exitB::accept)
            }
        }
        testObject.start()
        verify(enterA).accept(nodeA)

        testObject.transitionTo(nodeB)

        inOrder(enterB, enterEdgeAB, exitA, exitEdgeAB) {
            verify(exitA).accept(nodeA)
            verify(enterEdgeAB).accept(edgeAB)
        }
        verify(exitEdgeAB, never()).accept(edgeAB)
        verify(enterB, never()).accept(nodeB)
    }

    @Test
    fun `traversal fail but exit still desired`() {
        val enterA: NodeVisitor = mock()
        val exitA: NodeVisitor = mock()
        val enterB: NodeVisitor = mock()
        val exitB: NodeVisitor = mock()
        val enterEdgeAB: EdgeVisitor = mock()
        val exitEdgeAB: EdgeVisitor = mock()
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                onEnter(enterA::accept)
                onExit(exitA::accept)
                on(TestEvent) {
                    onEnter(enterEdgeAB::accept)
                    onExit(exitEdgeAB::accept)
                    transitionTo(StateB) {
                        it.failAndExit()
                    }
                }
            }
            state(StateB) {
                onEnter(enterB::accept)
                onExit(exitB::accept)
            }
        }
        testObject.start()
        verify(enterA).accept(nodeA)

        testObject.transitionTo(nodeB)

        inOrder(enterB, enterEdgeAB, exitA, exitEdgeAB) {
            verify(exitA).accept(nodeA)
            verify(enterEdgeAB).accept(edgeAB)
            verify(exitEdgeAB).accept(edgeAB)
        }
        verify(enterB, never()).accept(nodeB)
    }
    //endregion

    //region event driven transition
    @Test
    fun `traversal action is executed on event`() {
        val edgeAction: EdgeAction = mock()
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                on(TestEvent) {
                    transitionTo(StateB) {
                        edgeAction.execute(it)
                        it.success()
                    }
                }
            }
            state(StateB)
        }
        testObject.start()

        testObject.consume(TestEvent)

        verify(edgeAction).execute(isA())
    }

    @Test
    fun `event is ignored if we are in the wrong state`() {
        val edgeActionAB: EdgeAction = mock()
        val edgeActionBA: EdgeAction = mock()
        val testObject = graph {
            initialState(StateA)
            state(StateA) {
                on(TestEvent) {
                    transitionTo(StateB) {
                        edgeActionAB.execute(it)
                        it.success()
                    }
                }
            }
            state(StateB) {
                on(OtherTestEvent) {
                    transitionTo(StateA) {
                        edgeActionBA.execute(it)
                        it.success()
                    }
                }
            }
        }
        testObject.start()

        testObject.consume(OtherTestEvent)

        verifyZeroInteractions(edgeActionAB, edgeActionBA)
    }
    //endregion
}