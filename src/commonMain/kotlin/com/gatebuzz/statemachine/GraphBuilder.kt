@file:Suppress("unused")

package com.gatebuzz.statemachine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

fun graph(initBlock: GraphBuilder.() -> Unit): Graph {
    return GraphBuilder().apply(initBlock).build()
}

@DslMarker
annotation class GraphBuilderMarker

@DslMarker
annotation class StateBuilderMarker

@DslMarker
annotation class EdgeBuilderMarker

@Suppress("unused")
@GraphBuilderMarker
class GraphBuilder {
    private var initial: State? = null
    private var actionDispatcher: CoroutineDispatcher = Dispatchers.Default
    private val states: MutableList<StateBuilder> = mutableListOf()
    private val transitions: MutableList<(MachineState) -> Unit> = mutableListOf()
    private val stateChanges: MutableList<(State) -> Unit> = mutableListOf()

    fun build(): Graph {
        return Graph(dispatcher = actionDispatcher).apply {
            val allNodes = mutableMapOf<State, Node>().apply {
                states.forEach { putAll(it.allNodes()) }
            }

            states.forEach { sb ->
                allNodes[sb.id]?.let { n ->
                    sb.eventProducer?.let {
                        n.decision = Decision { state: State, trigger: Event? -> sb.eventProducer!!(state, trigger) }
                    }
                    sb.enter?.let { n.onEnter = it }
                    sb.exit?.let { n.onExit = it }
                }
            }
            allNodes.values.forEach { add(it) }

            initial?.let {
                val node = allNodes[initial]
                node?.let { initialState = MachineState.Dwelling(it) }
            }

            buildGraphEdges(allNodes)
        }
    }

    private fun Graph.buildGraphEdges(allNodes: MutableMap<State, Node>) {
        states.forEach { sb ->
            val from = allNodes[sb.id]!!
            sb.allowed.forEach { add(Edge(from, allNodes[it]!!)) }
            sb.edges.forEach { add(it.value.build(from, allNodes)) }
            sb.events.forEach { e ->
                with(e.value.build(from, allNodes)) {
                    add(this)
                    addEvent(e.key, this)
                }
            }
        }
    }

    fun state(id: State, initBlock: StateBuilder.() -> Unit = {}) {
        states.add(StateBuilder(id).apply(initBlock))
    }

    fun initialState(id: State) {
        initial = id
    }

    fun onTransition(action: (MachineState) -> Unit) {
        transitions.add(action)
    }

    fun onState(action: (State) -> Unit) {
        stateChanges.add(action)
    }
}

@StateBuilderMarker
class StateBuilder(val id: State) {
    internal val events: MutableMap<Event, EdgeBuilder> = mutableMapOf()
    internal val edges: MutableMap<State, EdgeBuilder> = mutableMapOf()
    internal var enter: StateVisitor? = null
    internal var exit: StateVisitor? = null
    internal var allowed: MutableList<State> = mutableListOf()
    internal var eventProducer: ((State, Event?) -> Event?)? = null

    fun <T : Event> on(event: T, initBlock: EdgeBuilder.() -> Unit) {
        events[event] = EdgeBuilder().apply(initBlock)
    }

    fun <T : State> onTransitionTo(state: T, initBlock: EdgeBuilder.() -> Unit) {
        edges[state] = EdgeBuilder(state).apply(initBlock)
    }

    fun decision(producer: (State, Event?) -> Event?) {
        this.eventProducer = producer
    }

    internal fun allNodes(): Map<State, Node> = mutableSetOf<Node>().apply {
        add(Node(id))
        addAll(allowed.map { Node(it) })
        addAll(edges.map { Node(it.value.destination!!) })
        addAll(events.map { Node(it.value.destination!!) })
    }.associateBy { it.id }

    fun onEnter(action: (State, Event?) -> Unit) {
        enter = StateVisitor { state, trigger -> action(state, trigger) }
    }

    fun onExit(action: (State, Event?) -> Unit) {
        exit = StateVisitor { state, trigger -> action(state, trigger) }
    }

    fun allows(vararg states: State) {
        allowed.addAll(states)
    }
}

@EdgeBuilderMarker
class EdgeBuilder(var destination: State? = null) {
    private var enter: EdgeVisitor? = null
    private var exit: EdgeVisitor? = null
    private var transitionAction: EdgeAction = { }

    fun transitionTo(id: State, action: EdgeAction = { }) {
        destination = id
        transitionAction = action
    }

    fun execute(action: EdgeAction) {
        transitionAction = action
    }

    fun onEnter(action: (Pair<State, State>) -> Unit) {
        enter = EdgeVisitor { action(it) }
    }

    fun onExit(action: (Pair<State, State>) -> Unit) {
        exit = EdgeVisitor { action(it) }
    }

    internal fun build(from: Node, allNodes: MutableMap<State, Node>): Edge {
        return Edge(from, allNodes[destination!!]!!).apply {
            enter?.let { onEnter = it }
            exit?.let { onExit = it }
            action = transitionAction
        }
    }
}
