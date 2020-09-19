package com.gatebuzz.statemachine

fun graph(initBlock: GraphBuilder.() -> Unit): Graph {
    return GraphBuilder().apply(initBlock).build()
}

@DslMarker
annotation class GraphBuilderMarker

@DslMarker
annotation class StateBuilderMarker

@DslMarker
annotation class EdgeBuilderMarker

@GraphBuilderMarker
class GraphBuilder {
    private var initial: State? = null
    private val states: MutableList<StateBuilder> = mutableListOf()
    private val transitions: MutableList<(MachineState) -> Unit> = mutableListOf()
    private val stateChanges: MutableList<(State) -> Unit> = mutableListOf()

    fun build(): Graph {
        return Graph().apply {
            val allNodes = mutableMapOf<State, Node>().apply {
                states.forEach { putAll(it.allNodes()) }
            }

            states.forEach { sb ->
                allNodes[sb.id]?.let { n ->
                    sb.eventProducer?.let { n.decision = Decision { node -> sb.eventProducer!!(node) } }
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

            addListeners()
        }
    }

    private fun Graph.buildGraphEdges(allNodes: MutableMap<State, Node>) {
        states.forEach { sb ->
            val from = allNodes[sb.id]!!
            sb.allowed.forEach { add(Edge(from, allNodes[it]!!)) }
            sb.edges.forEach { add(it.value.build(from)) }
            sb.events.forEach { e ->
                with(e.value.build(from)) {
                    add(this)
                    addEvent(e.key, this)
                }
            }
        }
    }

    private fun Graph.addListeners() {
        transitions.forEach {
            addStateChangeListener(object : StateTransitionListener {
                override fun onStateTransition(state: MachineState) = it(state)
            })
        }

        stateChanges.forEach {
            addStateListener(object : StateListener {
                override fun onState(state: State) = it(state)
            })
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
    val events: MutableMap<Event, EdgeBuilder> = mutableMapOf()
    val edges: MutableMap<State, EdgeBuilder> = mutableMapOf()
    var enter: NodeVisitor? = null
    var exit: NodeVisitor? = null
    var allowed: MutableList<State> = mutableListOf()
    var eventProducer: ((Node) -> Event?)? = null

    fun <T : Event> on(event: T, initBlock: EdgeBuilder.() -> Unit) {
        events[event] = EdgeBuilder().apply(initBlock)
    }

    fun <T : State> onTransitionTo(state: T, initBlock: EdgeBuilder.() -> Unit) {
        edges[state] = EdgeBuilder(state).apply(initBlock)
    }

    fun decision(producer: (Node)->Event?) {
        this.eventProducer = producer
    }

    fun allNodes(): Map<State, Node> = mutableSetOf<Node>().apply {
        add(Node(id))
        addAll(allowed.map { Node(it) })
        addAll(edges.map { Node(it.value.destination!!) })
        addAll(events.map { Node(it.value.destination!!) })
    }.associateBy { it.id }

    fun onEnter(action: (Node) -> Unit) {
        enter = NodeVisitor { action(it) }
    }

    fun onExit(action: (Node) -> Unit) {
        exit = NodeVisitor { action(it) }
    }

    fun allows(vararg states: State) {
        allowed.addAll(states)
    }
}

@EdgeBuilderMarker
class EdgeBuilder(var destination: State? = null) {
    var enter: EdgeVisitor? = null
    var exit: EdgeVisitor? = null
    var transitionAction: EdgeAction = EdgeAction { it.success() }

    fun transitionTo(id: State, action: (ResultEmitter) -> Unit = { it.success() }) {
        destination = id
        transitionAction = EdgeAction { action(it) }
    }

    fun execute(action: (ResultEmitter) -> Unit) {
        transitionAction = EdgeAction { action(it) }
    }

    fun onEnter(action: (Edge) -> Unit) {
        enter = EdgeVisitor { action(it) }
    }

    fun onExit(action: (Edge) -> Unit) {
        exit = EdgeVisitor { action(it) }
    }

    fun build(from: Node): Edge {
        return Edge(from, Node(destination!!)).apply {
            enter?.let { onEnter = it }
            exit?.let { onExit = it }
            action = transitionAction
        }
    }
}
