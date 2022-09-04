package com.gatebuzz.statemachine

import com.gatebuzz.statemachine.MachineState.Dwelling
import com.gatebuzz.statemachine.MachineState.Inactive
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext

typealias EdgeAction = suspend ActionResult.(Event?) -> Unit

internal class Node(val id: State) {
    val edgeTriggers: MutableMap<Event, Edge> = mutableMapOf()
    var onEnter: StateVisitor = StateVisitor { _, _ -> }
    var onExit: StateVisitor = StateVisitor { _, _ -> }
    var decision: Decision? = null
    var subgraph: Graph? = null

    override fun equals(other: Any?) = if (other is Node) other.id == id else false

    override fun hashCode(): Int = id.hashCode()
}

internal data class Edge internal constructor(val from: Node, val to: Node) {
    constructor(edge: Pair<State, State>) : this(Node(edge.first), Node(edge.second))

    var onEnter: EdgeVisitor = EdgeVisitor { }
    var onExit: EdgeVisitor = EdgeVisitor { }
    var action: EdgeAction = {}
}

internal fun interface EdgeVisitor {
    fun accept(edge: Pair<State, State>)
}

sealed class MachineState {
    abstract val id: State

    data class Inactive(override val id: State = InactiveState) : MachineState()

    data class Dwelling internal constructor(internal val node: Node, override val id: State = node.id) :
        MachineState() {
        constructor(state: State) : this(Node(state))
    }

    data class Traversing internal constructor(
        internal val edge: Edge,
        override val id: State = CompoundState(edge.from.id, edge.to.id),
        val trigger: Event? = null
    ) : MachineState()

    object InactiveState : State

    data class CompoundState(val from: State, val to: State) : State
}

class Graph internal constructor(
    var initialState: MachineState = Inactive(),
    var currentState: MachineState = Inactive(),
    val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val nodes: MutableList<Node> = mutableListOf()
    private val edges: MutableList<Edge> = mutableListOf()

    private val stateObserver: MutableSharedFlow<State> = MutableSharedFlow(replay = 0)
    private val machineStateObserver: MutableSharedFlow<MachineState> = MutableSharedFlow(replay = 0)

    fun observeState(): Flow<State> = stateObserver

    fun observeStateChanges(): Flow<MachineState> = machineStateObserver

    suspend fun start(startingState: MachineState = initialState): Graph {
        if (startingState is MachineState.Traversing) {
            throw IllegalArgumentException("Invalid initial state")
        }

        currentState = startingState
        if (currentState is Dwelling) {
            val state = currentState as Dwelling
            state.node.onEnter.accept(state.node.id, null)
        }

        CoroutineScope(dispatcher).run { notifyStateChange(currentState) }

        return this
    }

    suspend fun consume(event: Event) {
        if (currentState is Dwelling) {
            val state = currentState as Dwelling
            val validNode = findNode(state.id)
            validNode?.edgeTriggers?.get(event)?.let {
                moveViaEdge(it, event)
            }
        }
    }

    suspend fun transitionTo(state: State, trigger: Event? = null): State? {
        val validNode = findNode(state) ?: return null
        return doTransition(validNode, trigger)
    }

    internal suspend fun transitionTo(node: Node, trigger: Event? = null): State? {
        val validNode = findNode(node.id) ?: return null
        return doTransition(validNode, trigger)
    }

    internal fun findNode(id: State): Node? = nodes.find { it.id == id }

    internal fun add(state: State) {
        nodes.add(Node(state))
    }

    internal fun add(node: Node) {
        nodes.add(node)
    }

    internal fun add(edge: Pair<State, State>) {
        edges.add(Edge(Node(edge.first), Node(edge.second)))
    }

    internal fun add(edge: Edge) {
        edges.add(edge)
    }

    internal fun addEvent(event: Event, edge: Edge) {
        edge.from.edgeTriggers[event] = edge
    }

    private suspend fun doTransition(node: Node, trigger: Event?): State? = when (currentState) {
        is Dwelling -> moveViaEdge(Edge((currentState as Dwelling).node, node), trigger)
        is Inactive -> moveDirectly(node, trigger)
        else -> null
    }

    private suspend fun moveViaEdge(edge: Edge, trigger: Event?): State {
        val registeredEdge = edges.find { it == edge } ?: edge
        registeredEdge.from.onExit.accept(registeredEdge.from.id, trigger)

        val visibleEdge = registeredEdge.from.id to registeredEdge.to.id
        registeredEdge.onEnter.accept(visibleEdge)
        val captor = ActionResultCaptor()

        withContext(dispatcher) {
            registeredEdge.action(captor, trigger)
        }

        if (captor.success) {
            notifyStateChange(
                MachineState.Traversing(
                    edge = registeredEdge,
                    trigger = trigger
                )
            )
            registeredEdge.onExit.accept(visibleEdge)
            currentState = Dwelling(edge.to)
            notifyStateChange(currentState)
            if (registeredEdge.to.decision != null) {
                registeredEdge.to.decision?.decide(registeredEdge.to.id, trigger)?.let {
                    consume(it)
                }
            } else if (registeredEdge.to.subgraph != null) {
                val graph = registeredEdge.to.subgraph!!
                graph.start()
            } else {
                registeredEdge.to.onEnter.accept(registeredEdge.to.id, trigger)
            }
        } else {
            if (captor.andExit) {
                registeredEdge.onExit.accept(visibleEdge)
            }
            currentState = Dwelling(edge.from)
            registeredEdge.from.onEnter.accept(edge.from.id, trigger)
            notifyStateChange(currentState)
        }
        return registeredEdge.to.id
    }

    private suspend fun moveDirectly(node: Node, trigger: Event?): State {
        currentState = Dwelling(node.id)
        node.onEnter.accept(node.id, trigger)
        CoroutineScope(dispatcher).run {
            notifyStateChange(currentState)
        }
        return node.id
    }

    private suspend fun notifyStateChange(state: MachineState) {
        machineStateObserver.emit(state)
        if (state is Dwelling) {
            stateObserver.emit(state.id)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Graph) return false

        if (initialState != other.initialState) return false
        if (currentState != other.currentState) return false
        if (nodes.toSet() != other.nodes.toSet()) return false
        if (edges.toSet() != other.edges.toSet()) return false

        return true
    }

    override fun hashCode(): Int {
        var result = initialState.hashCode()
        result = 31 * result + currentState.hashCode()
        result = 31 * result + nodes.hashCode()
        result = 31 * result + edges.hashCode()
        return result
    }

    private inner class ActionResultCaptor : ActionResult {
        var success = true
        var andExit = false

        override fun fail() {
            success = false
        }

        override fun failAndExit() {
            success = false
            andExit = true
        }
    }
}
