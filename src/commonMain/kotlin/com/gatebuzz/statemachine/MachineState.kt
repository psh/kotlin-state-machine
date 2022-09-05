package com.gatebuzz.statemachine

@Suppress("unused")
sealed class MachineState {
    abstract val id: State

    data class Inactive(override val id: State = InactiveState) : MachineState()

    data class Dwelling constructor(val node: Node, override val id: State = node.id) :
        MachineState() {
        constructor(state: State) : this(Node(state))
    }

    data class Traversing constructor(
        val edge: Edge,
        override val id: State = CompoundState(edge.from.id, edge.to.id),
        val trigger: Event? = null
    ) : MachineState() {
        constructor(edge: Pair<State, State>, trigger: Event? = null) : this(
            edge = Edge(
                Node(edge.first),
                Node(edge.second)
            ),
            trigger = trigger
        )
    }

    object InactiveState : State

    data class CompoundState(val from: State, val to: State) : State
}
