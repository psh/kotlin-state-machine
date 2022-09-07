package com.gatebuzz.statemachine.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.gatebuzz.statemachine.Event
import com.gatebuzz.statemachine.Graph
import com.gatebuzz.statemachine.State
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
inline fun <reified T : State> Graph.collectAsState() =
    observe<T>().collectAsState(currentState.id)

fun Graph.postEvent(
    event: Event,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) = CoroutineScope(dispatcher).launch {
    consume(event)
}

fun Graph.postTransition(
    state: State,
    event: Event? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) = CoroutineScope(dispatcher).launch {
    this@postTransition.transitionTo(state, event)
}
