package com.gatebuzz.statemachine.impl

import com.gatebuzz.statemachine.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

internal suspend fun Graph.doStart(startingState: MachineState) {
    currentState = startingState
    if (currentState is MachineState.Dwelling) {
        val state = currentState as MachineState.Dwelling
        state.node.onEnter.accept(state.node.id, null)
    }

    notifyStateChange(currentState)
}

internal suspend fun Graph.doTransition(node: Node, trigger: Event?): State? = when (currentState) {
    is MachineState.Dwelling -> moveViaEdge(Edge((currentState as MachineState.Dwelling).node, node), trigger)
    is MachineState.Inactive -> moveDirectly(node, trigger)
    else -> null
}

internal suspend fun Graph.moveViaEdge(edge: Edge, trigger: Event?): State {
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
        currentState = MachineState.Dwelling(edge.to)
        notifyStateChange(currentState)
        if (registeredEdge.to.decision != null) {
            registeredEdge.to.decision?.decide(registeredEdge.to.id, trigger)?.let {
                consume(it)
            }
        } else {
            registeredEdge.to.onEnter.accept(registeredEdge.to.id, trigger)
        }
    } else {
        if (captor.andExit) {
            registeredEdge.onExit.accept(visibleEdge)
        }
        currentState = MachineState.Dwelling(edge.from)
        registeredEdge.from.onEnter.accept(edge.from.id, trigger)
        notifyStateChange(currentState)
    }
    return registeredEdge.to.id
}

internal suspend fun Graph.moveDirectly(node: Node, trigger: Event?): State {
    currentState = MachineState.Dwelling(node.id)
    node.onEnter.accept(node.id, trigger)
    CoroutineScope(dispatcher).run {
        notifyStateChange(currentState)
    }
    return node.id
}

internal suspend fun Graph.transitionTo(node: Node, trigger: Event? = null): State? {
    val validNode = findNode(node.id) ?: return null
    return doTransition(validNode, trigger)
}
internal fun Graph.findNode(id: State): Node? = nodes.find { it.id == id }

private suspend fun Graph.notifyStateChange(state: MachineState) {
    machineStateObserver.emit(state)
    if (state is MachineState.Dwelling) {
        stateObserver.emit(state.id)
    }
}

private class ActionResultCaptor : ActionResult {
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
