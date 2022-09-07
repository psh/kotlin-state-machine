package com.gatebuzz.statemachine

import app.cash.turbine.test
import com.gatebuzz.statemachine.TestState.*
import com.gatebuzz.statemachine.impl.*
import com.gatebuzz.verification.TestDecision
import com.gatebuzz.verification.TestEdgeVisitor
import com.gatebuzz.verification.TestStateVisitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
@ExperimentalTime
class GraphTest {

    private val onEnterA = TestStateVisitor()
    private val onExitA = TestStateVisitor()
    private val onEnterB = TestStateVisitor()
    private val onExitB = TestStateVisitor()
    private val onEnterC = TestStateVisitor()
    private val onExitC = TestStateVisitor()

    private val nodeA = Node(StateA).apply {
        onEnter = onEnterA
        onExit = onExitA
    }
    private val nodeB = Node(StateB).apply {
        onEnter = onEnterB
        onExit = onExitB
    }
    private val nodeC = Node(StateC).apply {
        onEnter = onEnterC
        onExit = onExitC
    }

    private val onEnterEdgeAB = TestEdgeVisitor()
    private val onExitEdgeAB = TestEdgeVisitor()
    private val onEnterEdgeBA = TestEdgeVisitor()
    private val onExitEdgeBA = TestEdgeVisitor()

    private val edgeAB = Edge(nodeA, nodeB).apply {
        onEnter = onEnterEdgeAB
        onExit = onExitEdgeAB
    }
    private val edgeBA = Edge(nodeB, nodeA).apply {
        onEnter = onEnterEdgeBA
        onExit = onExitEdgeBA
    }

    //region finding nodes, equality and hashcode
    @Test
    fun graphsAreEqualWhenEmpty() = runTest {
        assertEquals(Graph(), Graph())
    }

    @Test
    fun graphsAreEqualWithNodes() = runTest {
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
        assertEquals(testObject1, testObject2)
        assertEquals(testObject1.hashCode(), testObject2.hashCode())
    }

    @Test
    fun findNode() = runTest {
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
    fun emptyGraphStartsInactive() = runTest {
        val testObject = Graph()
        assertEquals(MachineState.Inactive(), testObject.initialState)
        assertEquals(MachineState.Inactive(), testObject.currentState)
    }

    @Test
    fun graphWithOneNodeIsInactiveUntilStartedObservingState() = runTest {
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
    fun graphWithOneNodeIsInactiveUntilStartedObservingTypeSafeState() = runTest {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
        }
        assertEquals(MachineState.Inactive(), testObject.currentState)

        testObject.observe<TestState>().test {
            testObject.start()

            assertEquals(MachineState.Dwelling(StateA), testObject.currentState)
            assertEquals(StateA, awaitItem())
        }
    }

    @Test
    fun graphWithOneNodeIsInactiveUntilStartedObservingMachineState() = runTest {
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
    fun transitionToNewState() = runTest {
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
    fun transitionToNewStateViaStateId() = runTest {
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
    fun cannotTransitionToNodesOutsideOfTheGraph() = runTest {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        testObject.start()

        testObject.observe<TestState>().test {
            val newNode = testObject.transitionTo(StateC)
            assertNull(newNode)
            assertEquals(MachineState.Dwelling(StateA), testObject.currentState)
            expectNoEvents()
        }
    }

    @Test
    fun observeStateChanges() = runTest {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }

        testObject.observe<TestState>().test {
            testObject.start()
            val firstItem: TestState = awaitItem()
            assertEquals(StateA, firstItem)

            testObject.transitionTo(StateB)
            val secondItem: TestState = awaitItem()
            assertEquals(StateB, secondItem)
        }
    }

    @Test
    fun observedMachineStateChangeIncludesTraversal() = runTest {
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
    fun stateEntryActionsAreExecutedWhenStarting() = runTest {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        testObject.start()

        assertTrue(onEnterA.wasCalled)
        assertEquals(StateA, onEnterA.state)
    }

    @Test
    fun stateEntryActionsAreExecutedWhenTraversing() = runTest {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        assertTrue(onEnterA wasCalledBefore onEnterB)
        assertEquals(StateA, onEnterA.state)
        assertEquals(StateB, onEnterB.state)
    }

    @Test
    fun traversalOnEnterIsExecuted() = runTest {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }
        testObject.start()

        testObject.transitionTo(nodeB)

        assertTrue(onEnterA wasCalledBefore onEnterEdgeAB && onEnterEdgeAB wasCalledBefore onEnterB)
        assertEquals(StateA, onEnterA.state)
        assertEquals(StateA to StateB, onEnterEdgeAB.edge)
        assertEquals(StateB, onEnterB.state)
    }

    @Test
    fun stateExitActionsAreExecutedWhenTraversing() = runTest {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        assertTrue(onExitA.wasCalled)
    }

    @Test
    fun traversalOnExitIsExecuted() = runTest {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        assertTrue(onExitA wasCalledBefore onExitEdgeAB)
        assertEquals(StateA, onExitA.state)
        assertEquals(StateA to StateB, onExitEdgeAB.edge)
    }

    @Test
    fun traversalEntryAndExit() = runTest {
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }
        testObject.start()
        assertTrue(onEnterA.wasCalled)
        assertEquals(StateA, onEnterA.state)

        testObject.transitionTo(StateB)

        assertTrue(
            onExitA wasCalledBefore onEnterEdgeAB && onEnterEdgeAB wasCalledBefore onExitEdgeAB &&
                onExitEdgeAB wasCalledBefore onEnterB
        )
        assertEquals(StateA, onExitA.state)
        assertEquals(StateA to StateB, onEnterEdgeAB.edge)
        assertEquals(StateA to StateB, onExitEdgeAB.edge)
        assertEquals(StateB, onEnterB.state)
    }
    //endregion

    //region traversal actions
    @Test
    fun traversalActionIsExecuted() = runTest {
        var executed = false
        edgeAB.action = { executed = true }
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        assertTrue(executed)
    }

    @Test
    fun traversalSucceeds() = runTest {
        edgeAB.action = { }
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }

        testObject.start()
        testObject.transitionTo(StateB)

        assertTrue(
            onExitA wasCalledBefore onEnterEdgeAB &&
                onEnterEdgeAB wasCalledBefore onExitEdgeAB &&
                onExitEdgeAB wasCalledBefore onEnterB
        )
        assertEquals(StateA, onExitA.state)
        assertEquals(StateA to StateB, onEnterEdgeAB.edge)
        assertEquals(StateA to StateB, onExitEdgeAB.edge)
        assertEquals(StateB, onEnterB.state)
    }

    @Test
    fun traversalFails() = runTest {
        edgeAB.action = { fail() }
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
        }

        testObject.start()
        testObject.transitionTo(StateB)

        assertEquals(StateA, onEnterA.state)
        assertEquals(StateA, onExitA.state)
        assertEquals(StateA to StateB, onEnterEdgeAB.edge)

        assertTrue(onEnterB.wasNotCalled)
        assertTrue(onExitEdgeAB.wasNotCalled)
    }
    //endregion

    //region event driven transition
    @Test
    fun traversalActionIsExecutedOnEvent() = runTest {
        var actionCalled = false
        edgeAB.action = { actionCalled = true }
        val event: Event = object : Event {}
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
            edgeAB.from.edgeTriggers[event] = edgeAB
        }
        testObject.start()

        testObject.consume(event)

        assertTrue(actionCalled)
    }

    @Test
    fun eventIsIgnoredIfWeAreInTheWrongState() = runTest {
        var action1Called = false
        var action2Called = false
        edgeAB.action = object : EdgeAction {
            override suspend fun invoke(p1: ActionResult, p2: Event?) {
                action1Called = true
            }
        }
        edgeBA.action = object : EdgeAction {
            override suspend fun invoke(p1: ActionResult, p2: Event?) {
                action2Called = true
            }
        }
        val event: Event = object : Event {}
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
            add(edgeBA)
            edgeBA.from.edgeTriggers[event] = edgeBA
        }
        testObject.start()

        testObject.consume(event)

        assertFalse(action1Called)
        assertFalse(action2Called)
    }

    @Test
    fun observedMachineStateChangeIncludesTraversalWhenConsumingEvent() = runTest {
        val event: Event = object : Event {}
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(edgeAB)
            edgeAB.from.edgeTriggers[event] = edgeAB
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
    fun decisionsTakePrecedenceOverOnEnter() = runTest {
        val decision = TestDecision(result = null)
        nodeB.decision = decision
        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
        }
        testObject.start()

        testObject.transitionTo(StateB)

        assertTrue(onEnterA wasCalledBefore decision)
        assertEquals(StateA, onEnterA.state)
        assertEquals(StateB, decision.state)
        assertTrue(onEnterB.wasNotCalled)
    }

    @Test
    fun decisionsTriggerEvents() = runTest {
        val decision = TestDecision(result = TestEvents.OtherTestEvent)
        nodeB.decision = decision

        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(nodeC)
            val edge = Edge(nodeB, nodeC)
            edge.from.edgeTriggers[TestEvents.OtherTestEvent] = edge
        }
        testObject.start()

        testObject.transitionTo(StateB)

        assertTrue(
            onEnterA wasCalledBefore onExitA &&
                onExitA wasCalledBefore decision &&
                decision wasCalledBefore onExitB &&
                onExitB wasCalledBefore onEnterC
        )
        assertEquals(StateA, onEnterA.state)
        assertEquals(StateB, decision.state)
        assertEquals(StateB, onExitB.state)
        assertEquals(StateC, onEnterC.state)

        assertEquals(TestEvents.OtherTestEvent, onExitB.trigger)
        assertEquals(TestEvents.OtherTestEvent, onEnterC.trigger)

        assertTrue(onEnterB.wasNotCalled)
    }

    @Test
    fun nullDecisionsHaveNoEffect() = runTest {
        val decision = TestDecision(result = null)
        nodeB.decision = decision

        val testObject = Graph().apply {
            initialState = MachineState.Dwelling(nodeA)
            add(nodeA)
            add(nodeB)
            add(nodeC)
            val edge = Edge(StateB to StateC)
            edge.from.edgeTriggers[TestEvents.OtherTestEvent] = edge
        }
        testObject.start()

        testObject.transitionTo(StateB)

        assertTrue(
            onEnterA wasCalledBefore onExitA &&
                onExitA wasCalledBefore decision
        )
        assertTrue(onEnterB.wasNotCalled)
        assertTrue(onExitB.wasNotCalled)
        assertTrue(onEnterC.wasNotCalled)
    }

    //endregion
}
