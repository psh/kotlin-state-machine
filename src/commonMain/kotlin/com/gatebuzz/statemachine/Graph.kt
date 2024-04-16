package com.gatebuzz.statemachine

import com.gatebuzz.statemachine.MachineState.Inactive
import com.gatebuzz.statemachine.impl.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

fun graph(initBlock: GraphBuilder.() -> Unit): Graph {
    return GraphBuilder().apply(initBlock).build()
}

@Suppress("MemberVisibilityCanBePrivate")
class Graph internal constructor(
    var initialState: MachineState = Inactive(),
    var currentState: MachineState = Inactive(),
    val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    val currentStateName: String get() = currentState.id::class.simpleName ?: "Unknown"
    internal val nodes: MutableList<Node> = mutableListOf()
    internal val edges: MutableList<Edge> = mutableListOf()
    internal val stateObserver = MutableSharedFlow<State>()
    internal val machineStateObserver = MutableSharedFlow<MachineState>()

    inline fun <reified T : State> observe(): Flow<T> {
        @Suppress("UNCHECKED_CAST")
        return observeState() as Flow<T>
    }

    fun observeState(): Flow<State> = stateObserver

    fun observeStateChanges(): Flow<MachineState> = machineStateObserver

    suspend fun start(startingState: MachineState = initialState): Graph {
        if (startingState is MachineState.Traversing) {
            throw IllegalArgumentException("Invalid initial state")
        }

        doStart(startingState)

        return this
    }

    suspend fun consume(event: Event) {
        if (currentState is MachineState.Dwelling) {
            val state = currentState as MachineState.Dwelling
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

    override fun toString(): String = buildString {
        append(currentState::class.simpleName)
        if (currentState is MachineState.Dwelling) {
            append(" on $currentStateName")
        } else if (currentState is MachineState.Traversing) {
            val state = currentState as MachineState.Traversing
            append(" from ${state.edge.from.id::class.simpleName}")
            append(" -> ${state.edge.to.id::class.simpleName}")
        }
    }
}

class Node(val id: State) {
    val edgeTriggers: MutableMap<Event, Edge> = mutableMapOf()
    var onEnter: StateVisitor = StateVisitor { _, _ -> }
    var onExit: StateVisitor = StateVisitor { _, _ -> }
    var decision: Decision? = null

    override fun equals(other: Any?) = if (other is Node) other.id == id else false

    override fun hashCode(): Int = id.hashCode()
}

data class Edge(val from: Node, val to: Node) {
    constructor(edge: Pair<State, State>) : this(Node(edge.first), Node(edge.second))

    var onEnter: EdgeVisitor = EdgeVisitor { }
    var onExit: EdgeVisitor = EdgeVisitor { }
    var action: EdgeAction = {}
}

fun interface EdgeVisitor {
    fun accept(edge: Pair<State, State>)
}
