package com.gatebuzz.statemachine

import com.gatebuzz.statemachine.MachineState.Dwelling
import com.gatebuzz.statemachine.MachineState.Inactive

data class Node(val id: State) {
    var onEnter: NodeVisitor = NodeVisitor { _, _ -> }
    var onExit: NodeVisitor = NodeVisitor { _, _ -> }
    var decision: Decision? = null
}

data class Edge(val from: Node, val to: Node) {
    var onEnter: EdgeVisitor = EdgeVisitor { }
    var onExit: EdgeVisitor = EdgeVisitor { }
    var action: EdgeAction = EdgeAction { trigger, result -> result.success(trigger) }
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
            state.node.onEnter.accept(state.node, null)
        }
        notifyStateChange(currentState)
        return this
    }

    fun consume(event: Event) {
        val edge = edgeTriggers[event]
        edge?.let {
            if (currentState is Dwelling && (currentState as Dwelling).node == edge.from) {
                moveViaEdge(edge, event)
            }
        }
    }

    fun transitionTo(state: State, trigger: Event? = null): Node? {
        val node = findNode(state) ?: return null
        return doTransition(node, trigger)
    }

    fun transitionTo(node: Node, trigger: Event? = null): Node? {
        val validNode = findNode(node.id) ?: return null
        return doTransition(validNode, trigger)
    }

    fun addStateChangeListener(listener: StateTransitionListener) {
        listeners.add(listener)
    }

    fun addStateListener(listener: StateListener) {
        listeners.add(
            object : StateTransitionListener {
                override fun onStateTransition(state: MachineState) {
                    if (state is Dwelling) {
                        listener.onState(state.id)
                    }
                }
            }
        )
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

    private fun doTransition(node: Node, trigger: Event?): Node? {
        return when (currentState) {
            is Dwelling -> moveViaEdge(Edge((currentState as Dwelling).node, node), trigger)
            is Inactive -> moveDirectly(node, trigger)
            else -> null
        }
    }

    private fun moveViaEdge(edge: Edge, trigger: Event?): Node {
        val registeredEdge = edges.find { it == edge } ?: edge
        registeredEdge.from.onExit.accept(registeredEdge.from, trigger)

        registeredEdge.onEnter.accept(registeredEdge)
        registeredEdge.action.execute(
            trigger,
            object : ResultEmitter {
                override fun success(trigger: Event?) {
                    notifyStateChange(MachineState.Traversing(registeredEdge))
                    registeredEdge.onExit.accept(registeredEdge)
                    currentState = Dwelling(edge.to)
                    notifyStateChange(currentState)
                    if (edge.to.decision != null) {
                        edge.to.decision?.decide(edge.to, trigger)?.let {
                            consume(it)
                        }
                    } else {
                        edge.to.onEnter.accept(edge.to, trigger)
                    }
                }

                override fun failAndExit(trigger: Event?) {
                    registeredEdge.onExit.accept(registeredEdge)
                    failure(trigger)
                }

                override fun failure(trigger: Event?) {
                    currentState = Dwelling(edge.from)
                    edge.from.onEnter.accept(edge.from, trigger)
                    notifyStateChange(currentState)
                }
            }
        )
        return edge.to
    }

    private fun moveDirectly(node: Node, trigger: Event?): Node {
        currentState = Dwelling(node)
        node.onEnter.accept(node, trigger)
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
