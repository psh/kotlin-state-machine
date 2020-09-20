package com.gatebuzz.statemachine

import com.gatebuzz.statemachine.TestEvents.OtherTestEvent
import com.gatebuzz.statemachine.TestState.*
import com.nhaarman.mockitokotlin2.*
import org.junit.Assert.*
import org.junit.Test

class GraphTest {

    private val transitionListener: StateTransitionListener = mock()
    private val stateListener: StateListener = mock()
    private val nodeA = Node(StateA)
    private val nodeB = Node(StateB)
    private val nodeC = Node(StateC)
    private val edgeAB = Edge(nodeA, nodeB)
    private val edgeBA = Edge(nodeB, nodeA)

    //region finding nodes, equality and hashcode
    @Test
    fun `graphs are equal when empty`() {
        assertTrue(Graph() == Graph())
    }

    @Test
    fun `graphs are equal and ignore listeners`() {
        val testObject1 = Graph().apply {
            initialState = MachineState.Dwelling(Node(StateA))
            add(Node(StateA))
            add(Node(StateB))
            add(Edge(Node(StateA), Node(StateB)))
        }
        val testObject2 = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
            addStateChangeListener(transitionListener)
            addStateListener(stateListener)
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
    fun `graph with one node is inactive until started`() {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            addStateChangeListener(transitionListener)
            addStateListener(stateListener)
        }
        assertEquals(MachineState.Inactive(), testObject.currentState)

        testObject.start()

        assertEquals(MachineState.Dwelling(nodeA), testObject.currentState)
        transitionListener.onStateTransition(MachineState.Dwelling(nodeA))
        stateListener.onState(StateA)
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

        val newNode = testObject.transitionTo(nodeB)

        assertEquals(nodeB, newNode)
        assertEquals(MachineState.Dwelling(nodeB), testObject.currentState)
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

        assertEquals(nodeB, newNode)
        assertEquals(MachineState.Dwelling(nodeB), testObject.currentState)
    }

    @Test
    fun `cannot transition to nodes outside of the graph`() {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        testObject.start()

        testObject.addStateChangeListener(transitionListener)
        testObject.addStateListener(stateListener)
        val newNode = testObject.transitionTo(nodeC)

        assertNull(newNode)
        assertEquals(MachineState.Dwelling(nodeA), testObject.currentState)
        verifyZeroInteractions(transitionListener, stateListener)
    }

    @Test
    fun `state change includes traversal`() {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            addStateChangeListener(transitionListener)
            addStateListener(stateListener)
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        inOrder(transitionListener) {
            verify(transitionListener).onStateTransition(MachineState.Dwelling(nodeA))
            verify(transitionListener).onStateTransition(MachineState.Traversing(Edge(nodeA, nodeB)))
            verify(transitionListener).onStateTransition(MachineState.Dwelling(nodeB))
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
        nodeA.onEnter = mock()
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        testObject.start()

        verify(nodeA.onEnter).accept(nodeA, null)
    }

    @Test
    fun `state entry actions are executed when traversing`() {
        nodeA.onEnter = mock()
        nodeB.onEnter = mock()
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        inOrder(nodeA.onEnter, nodeB.onEnter) {
            verify(nodeA.onEnter).accept(nodeA, null)
            verify(nodeB.onEnter).accept(nodeB, null)
        }
    }

    @Test
    fun `traversal on enter is executed`() {
        nodeA.onEnter = mock()
        nodeB.onEnter = mock()
        edgeAB.onEnter = mock()
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        inOrder(nodeA.onEnter, nodeB.onEnter, edgeAB.onEnter) {
            verify(nodeA.onEnter).accept(nodeA, null)
            verify(edgeAB.onEnter).accept(edgeAB)
            verify(nodeB.onEnter).accept(nodeB, null)
        }
    }

    @Test
    fun `state exit actions are executed when traversing`() {
        nodeA.onExit = mock()
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        verify(nodeA.onExit).accept(nodeA, null)
    }

    @Test
    fun `traversal on exit is executed`() {
        nodeA.onExit = mock()
        nodeB.onExit = mock()
        edgeAB.onExit = mock()
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        inOrder(nodeA.onExit, nodeB.onExit, edgeAB.onExit) {
            verify(nodeA.onExit).accept(nodeA, null)
            verify(edgeAB.onExit).accept(edgeAB)
        }
    }

    @Test
    fun `traversal entry and exit`() {
        nodeA.onEnter = mock()
        nodeA.onExit = mock()
        nodeB.onEnter = mock()
        nodeB.onExit = mock()
        edgeAB.onEnter = mock()
        edgeAB.onExit = mock()
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }
        testObject.start()
        verify(nodeA.onEnter).accept(nodeA, null)

        testObject.transitionTo(nodeB)

        inOrder(nodeB.onEnter, edgeAB.onEnter, nodeA.onExit, nodeB.onExit, edgeAB.onExit) {
            verify(nodeA.onExit).accept(nodeA, null)
            verify(edgeAB.onEnter).accept(edgeAB)
            verify(edgeAB.onExit).accept(edgeAB)
            verify(nodeB.onEnter).accept(nodeB, null)
        }
    }
    //endregion

    //region traversal actions
    @Test
    fun `traversal action is executed`() {
        edgeAB.action = mock()
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        verify(edgeAB.action).execute(anyOrNull(), isA())
    }

    @Test
    fun `traversal succeeds`() {
        nodeA.onEnter = mock()
        nodeA.onExit = mock()
        nodeA.onEnter = mock()
        nodeA.onExit = mock()
        nodeB.onEnter = mock()
        nodeB.onExit = mock()
        edgeAB.onEnter = mock()
        edgeAB.onExit = mock()
        edgeAB.action = EdgeAction { tr, result -> result.success(tr) }
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }

        testObject.start()
        testObject.transitionTo(nodeB)

        inOrder(nodeB.onEnter, edgeAB.onEnter, nodeA.onExit, nodeB.onExit, edgeAB.onExit) {
            verify(nodeA.onExit).accept(nodeA, null)

            verify(edgeAB.onEnter).accept(edgeAB)
            verify(edgeAB.onExit).accept(edgeAB)

            verify(nodeB.onEnter).accept(nodeB, null)
        }
    }

    @Test
    fun `traversal fails`() {
        nodeA.onEnter = mock()
        nodeA.onExit = mock()
        nodeA.onEnter = mock()
        nodeA.onExit = mock()
        nodeB.onEnter = mock()
        nodeB.onExit = mock()
        edgeAB.onEnter = mock()
        edgeAB.onExit = mock()
        edgeAB.action = EdgeAction { tr, result -> result.failure(tr) }
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }

        testObject.start()
        testObject.transitionTo(nodeB)

        inOrder(nodeA.onEnter, nodeB.onEnter, edgeAB.onEnter, nodeA.onExit, nodeB.onExit, edgeAB.onExit) {
            verify(nodeA.onEnter).accept(nodeA, null)
            verify(nodeA.onExit).accept(nodeA, null)

            verify(edgeAB.onEnter).accept(edgeAB)
            verify(nodeA.onEnter).accept(nodeA, null)
        }
        verifyZeroInteractions(nodeB.onEnter)
        verifyZeroInteractions(edgeAB.onExit)
    }
    //endregion

    //region event driven transition
    @Test
    fun `traversal action is executed on event`() {
        edgeAB.action = mock()
        val event: Event = mock()
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
            addEvent(event, edgeAB)
        }
        testObject.start()

        testObject.consume(event)

        verify(edgeAB.action).execute(anyOrNull(), isA())
    }

    @Test
    fun `event is ignored if we are in the wrong state`() {
        edgeAB.action = mock()
        edgeBA.action = mock()
        val event: Event = mock()
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

        verifyZeroInteractions(edgeAB.action, edgeBA.action)
    }
    //endregion

    //region decision states
    @Test
    fun `decisions take precedence over onEnter`() {
        nodeA.onEnter = mock()
        nodeB.decision = mock()
        nodeB.onEnter = mock()
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        inOrder(nodeA.onEnter, nodeB.decision!!) {
            verify(nodeA.onEnter).accept(nodeA, null)
            verify(nodeB.decision)!!.decide(nodeB, null)
        }
        verify(nodeB.onEnter, never()).accept(any(), isNull())
    }

    @Test
    fun `decisions trigger events`() {
        nodeA.onEnter = mock()
        nodeB.decision = mock()
        nodeB.onEnter = mock()
        nodeB.onExit = mock()
        nodeC.onEnter = mock()
        whenever(nodeB.decision!!.decide(anyOrNull(), anyOrNull())).thenReturn(OtherTestEvent)

        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(nodeC)
            addEvent(OtherTestEvent, Edge(nodeB, nodeC))
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        inOrder(nodeA.onEnter, nodeB.decision!!, nodeB.onExit, nodeC.onEnter) {
            verify(nodeA.onEnter).accept(nodeA, null)
            verify(nodeB.decision)!!.decide(nodeB, null)
            verify(nodeB.onExit).accept(nodeB, OtherTestEvent)
            verify(nodeC.onEnter).accept(nodeC, OtherTestEvent)
        }
        verify(nodeB.onEnter, never()).accept(any(), anyOrNull())
    }

    @Test
    fun `null decisions have no effect`() {
        nodeA.onEnter = mock()
        nodeB.decision = mock()
        nodeB.onEnter = mock()
        nodeB.onExit = mock()
        nodeC.onEnter = mock()
        whenever(nodeB.decision!!.decide(anyOrNull(), anyOrNull())).thenReturn(null)

        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(nodeC)
            addEvent(OtherTestEvent, Edge(nodeB, nodeC))
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        inOrder(nodeA.onEnter, nodeB.decision!!) {
            verify(nodeA.onEnter).accept(nodeA, null)
            verify(nodeB.decision)!!.decide(nodeB, null)
        }
        verify(nodeB.onEnter, never()).accept(anyOrNull(), anyOrNull())
        verify(nodeB.onExit, never()).accept(nodeB, null)
        verify(nodeC.onEnter, never()).accept(nodeC, null)
    }

    //endregion
}
