import AppEvents.LoginEvent
import AppEvents.LogoutEvent
import AppState.Home
import AppState.Login
import com.gatebuzz.statemachine.Event
import com.gatebuzz.statemachine.State
import com.gatebuzz.statemachine.graph

val appGraph = graph {
    initialState(Login)

    state(Login) {
        on(LoginEvent) {
            transitionTo(Home)
        }
    }

    state(Home) {
        on(LogoutEvent) {
            transitionTo(Login)
        }
    }
}

sealed class AppState : State {
    object Login : AppState()
    object Home : AppState()
}

sealed class AppEvents : Event {
    object LoginEvent : AppEvents()
    object LogoutEvent : AppEvents()
}
