package com.gatebuzz.statemachine

import com.gatebuzz.statemachine.TestEvents.OtherTestEvent
import com.gatebuzz.statemachine.TestState.*
import io.mockk.*
import org.junit.Assert.*
import org.junit.Test

class GraphTest {

    private val transitionListener: StateTransitionListener = mockk(relaxed = true)
    private val stateListener: StateListener = mockk(relaxed = true)
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
        verify { listOf(transitionListener, stateListener) wasNot Called }
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

        verifyOrder {
            transitionListener.onStateTransition(MachineState.Dwelling(nodeA))
            transitionListener.onStateTransition(MachineState.Traversing(Edge(nodeA, nodeB)))
            transitionListener.onStateTransition(MachineState.Dwelling(nodeB))
        }
        verifyOrder {
            stateListener.onState(StateA)
            stateListener.onState(StateB)
        }
    }
    //endregion

    //region entry & exit actions
    @Test
    fun `state entry actions are executed when starting`() {
        nodeA.onEnter = mockk(relaxed = true)
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        testObject.start()

        verify { nodeA.onEnter.accept(nodeA, null) }
    }

    @Test
    fun `state entry actions are executed when traversing`() {
        nodeA.onEnter = mockk(relaxed = true)
        nodeB.onEnter = mockk(relaxed = true)
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        verifyOrder {
            nodeA.onEnter.accept(nodeA, null)
            nodeB.onEnter.accept(nodeB, null)
        }
    }

    @Test
    fun `traversal on enter is executed`() {
        nodeA.onEnter = mockk(relaxed = true)
        nodeB.onEnter = mockk(relaxed = true)
        edgeAB.onEnter = mockk(relaxed = true)
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        verifyOrder {
            nodeA.onEnter.accept(nodeA, null)
            edgeAB.onEnter.accept(edgeAB)
            nodeB.onEnter.accept(nodeB, null)
        }
    }

    @Test
    fun `state exit actions are executed when traversing`() {
        nodeA.onExit = mockk(relaxed = true)
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        verify { nodeA.onExit.accept(nodeA, null) }
    }

    @Test
    fun `traversal on exit is executed`() {
        nodeA.onExit = mockk(relaxed = true)
        nodeB.onExit = mockk(relaxed = true)
        edgeAB.onExit = mockk(relaxed = true)
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        verifyOrder {
            nodeA.onExit.accept(nodeA, null)
            edgeAB.onExit.accept(edgeAB)
        }
    }

    @Test
    fun `traversal entry and exit`() {
        nodeA.onEnter = mockk(relaxed = true)
        nodeA.onExit = mockk(relaxed = true)
        nodeB.onEnter = mockk(relaxed = true)
        nodeB.onExit = mockk(relaxed = true)
        edgeAB.onEnter = mockk(relaxed = true)
        edgeAB.onExit = mockk(relaxed = true)
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }
        testObject.start()
        verify { nodeA.onEnter.accept(nodeA, null) }

        testObject.transitionTo(nodeB)

        verifyOrder {
            nodeA.onExit.accept(nodeA, null)
            edgeAB.onEnter.accept(edgeAB)
            edgeAB.onExit.accept(edgeAB)
            nodeB.onEnter.accept(nodeB, null)
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

        testObject.transitionTo(nodeB)

        verify { edgeAB.action.execute(any(), ofType()) }
    }

    @Test
    fun `traversal succeeds`() {
        nodeA.onEnter = mockk(relaxed = true)
        nodeA.onExit = mockk(relaxed = true)
        nodeA.onEnter = mockk(relaxed = true)
        nodeA.onExit = mockk(relaxed = true)
        nodeB.onEnter = mockk(relaxed = true)
        nodeB.onExit = mockk(relaxed = true)
        edgeAB.onEnter = mockk(relaxed = true)
        edgeAB.onExit = mockk(relaxed = true)
        edgeAB.action = EdgeAction { tr, result -> result.success(tr) }
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }

        testObject.start()
        testObject.transitionTo(nodeB)

        verifyOrder {
            nodeA.onExit.accept(nodeA, null)

            edgeAB.onEnter.accept(edgeAB)
            edgeAB.onExit.accept(edgeAB)

            nodeB.onEnter.accept(nodeB, null)
        }
    }

    @Test
    fun `traversal fails`() {
        nodeA.onEnter = mockk(relaxed = true)
        nodeA.onExit = mockk(relaxed = true)
        nodeA.onEnter = mockk(relaxed = true)
        nodeA.onExit = mockk(relaxed = true)
        nodeB.onEnter = mockk(relaxed = true)
        nodeB.onExit = mockk(relaxed = true)
        edgeAB.onEnter = mockk(relaxed = true)
        edgeAB.onExit = mockk(relaxed = true)
        edgeAB.action = EdgeAction { tr, result -> result.failure(tr) }
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }

        testObject.start()
        testObject.transitionTo(nodeB)

        verifyOrder {
            nodeA.onEnter.accept(nodeA, null)
            nodeA.onExit.accept(nodeA, null)

            edgeAB.onEnter.accept(edgeAB)
            nodeA.onEnter.accept(nodeA, null)
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
    //endregion

    //region decision states
    @Test
    fun `decisions take precedence over onEnter`() {
        nodeA.onEnter = mockk(relaxed = true)
        nodeB.decision = mockk(relaxed = true)
        nodeB.onEnter = mockk(relaxed = true)
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        verifyOrder {
            nodeA.onEnter.accept(nodeA, null)
            nodeB.decision!!.decide(nodeB, null)
        }
        verify(exactly = 0) { nodeB.onEnter.accept(any(), isNull()) }
    }

    @Test
    fun `decisions trigger events`() {
        nodeA.onEnter = mockk(relaxed = true)
        nodeB.decision = mockk(relaxed = true)
        nodeB.onEnter = mockk(relaxed = true)
        nodeB.onExit = mockk(relaxed = true)
        nodeC.onEnter = mockk(relaxed = true)
        every { nodeB.decision!!.decide(any(), any()) } returns OtherTestEvent

        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(nodeC)
            addEvent(OtherTestEvent, Edge(nodeB, nodeC))
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        verifyOrder {
            nodeA.onEnter.accept(nodeA, null)
            nodeB.decision!!.decide(nodeB, null)
            nodeB.onExit.accept(nodeB, OtherTestEvent)
            nodeC.onEnter.accept(nodeC, OtherTestEvent)
        }
        verify(exactly = 0) { nodeB.onEnter.accept(any(), any()) }
    }

    @Test
    fun `null decisions have no effect`() {
        nodeA.onEnter = mockk(relaxed = true)
        nodeB.decision = mockk(relaxed = true)
        nodeB.onEnter = mockk(relaxed = true)
        nodeB.onExit = mockk(relaxed = true)
        nodeC.onEnter = mockk(relaxed = true)
        every { nodeB.decision!!.decide(any(), any()) } returns null

        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(nodeC)
            addEvent(OtherTestEvent, Edge(nodeB, nodeC))
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        verifyOrder {
            nodeA.onEnter.accept(nodeA, null)
            nodeB.decision!!.decide(nodeB, null)
        }
        verify(exactly = 0) { nodeB.onEnter.accept(any(), any()) }
        verify(exactly = 0) { nodeB.onExit.accept(nodeB, null) }
        verify(exactly = 0) { nodeC.onEnter.accept(nodeC, null) }
    }

    //endregion
}
