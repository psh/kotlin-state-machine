package com.gatebuzz.statemachine

import com.gatebuzz.statemachine.MachineState.Dwelling
import com.gatebuzz.statemachine.MachineState.Inactive

data class Node(val id: State) {
    var onEnter: NodeVisitor = NodeVisitor { }
    var onExit: NodeVisitor = NodeVisitor { }
}

data class Edge(val from: Node, val to: Node) {
    var onEnter: EdgeVisitor = EdgeVisitor { }
    var onExit: EdgeVisitor = EdgeVisitor { }
    var action: EdgeAction = EdgeAction(ResultEmitter::success)
}

sealed class MachineState {
    abstract val id: State

    data class Inactive(override val id: State = InactiveState) : MachineState()

    data class Dwelling(val node: Node, override val id: State = node.id) : MachineState()

    data class Traversing(val edge: Edge, override val id: State = CompoundState(edge.from.id, edge.to.id)) :
        MachineState()

    object InactiveState : State
    data class CompoundState(val from: State, val to: State) : State
}

class Graph internal constructor(
    var initialState: MachineState = Inactive(),
    var currentState: MachineState = Inactive()
) {

    private val nodes: MutableList<Node> = mutableListOf()
    private val edges: MutableList<Edge> = mutableListOf()
    private val listeners: MutableList<StateTransitionListener> = mutableListOf()
    private val edgeTriggers: MutableMap<Event, Edge> = mutableMapOf()

    fun findNode(id: State): Node? = nodes.find { it.id == id }

    fun start(startingState: MachineState = initialState): Graph {
        if (startingState is MachineState.Traversing) {
            throw IllegalArgumentException("Invalid initial state")
        }

        currentState = startingState
        if (currentState is Dwelling) {
            val state = currentState as Dwelling
            state.node.onEnter.accept(state.node)
        }
        notifyStateChange(currentState)
        return this
    }

    fun consume(event: Event) {
        val edge = edgeTriggers[event]
        edge?.let {
            if (currentState is Dwelling && (currentState as Dwelling).node == edge.from) {
                moveViaEdge(edge)
            }
        }
    }

    fun transitionTo(state: State): Node? {
        val node = findNode(state) ?: return null
        return doTransition(node)
    }

    fun transitionTo(node: Node): Node? {
        val validNode = findNode(node.id) ?: return null
        return doTransition(validNode)
    }

    fun addStateChangeListener(listener: StateTransitionListener) {
        listeners.add(listener)
    }

    fun addStateListener(listener: StateListener) {
        listeners.add(object : StateTransitionListener {
            override fun onStateTransition(state: MachineState) {
                if (state is Dwelling) {
                    listener.onState(state.id)
                }
            }
        })
    }

    internal fun add(node: Node) {
        nodes.add(node)
    }

    internal fun add(edge: Edge) {
        edges.add(edge)
    }

    internal fun addEvent(event: Event, edge: Edge) {
        edgeTriggers[event] = edge
    }

    private fun doTransition(node: Node): Node? {
        return when (currentState) {
            is Dwelling -> moveViaEdge(Edge((currentState as Dwelling).node, node))
            is Inactive -> moveDirectly(node)
            else -> null
        }
    }

    private fun moveViaEdge(edge: Edge): Node {
        val registeredEdge = edges.find { it == edge } ?: edge
        registeredEdge.from.onExit.accept(registeredEdge.from)

        registeredEdge.onEnter.accept(registeredEdge)
        registeredEdge.action.execute(object : ResultEmitter {
            override fun success() {
                notifyStateChange(MachineState.Traversing(registeredEdge))
                registeredEdge.onExit.accept(registeredEdge)
                currentState = Dwelling(edge.to).apply {
                    edge.to.onEnter.accept(edge.to)
                }
                notifyStateChange(currentState)
            }

            override fun failure() {
                currentState = Dwelling(edge.from).apply {
                    edge.from.onEnter.accept(edge.from)
                }
                notifyStateChange(currentState)
            }
        })
        return edge.to
    }

    private fun moveDirectly(node: Node): Node {
        currentState = Dwelling(node)
        node.onEnter.accept(node)
        notifyStateChange(currentState)
        return node
    }

    private fun notifyStateChange(state: MachineState) = listeners.forEach {
        it.onStateTransition(state)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Graph

        if (initialState != other.initialState) return false
        if (currentState != other.currentState) return false
        if (nodes.toSet() != other.nodes.toSet()) return false
        if (edges.toSet() != other.edges.toSet()) return false
        if (edgeTriggers != other.edgeTriggers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = initialState.hashCode()
        result = 31 * result + currentState.hashCode()
        result = 31 * result + nodes.hashCode()
        result = 31 * result + edges.hashCode()
        result = 31 * result + edgeTriggers.hashCode()
        return result
    }
}