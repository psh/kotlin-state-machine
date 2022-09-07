import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import com.gatebuzz.statemachine.compose.collectAsState
import com.gatebuzz.statemachine.compose.postEvent
import com.gatebuzz.statemachine.compose.postTransition

@Composable
@Preview
fun App() {
    val appState = appGraph.collectAsState<AppState>()

    MaterialTheme {
        when (appState.value) {
            AppState.Home -> Home()
            AppState.Login -> Login()
        }
    }
}

@Composable
fun Login() {
    Button(onClick = {
        // fire off a login event to cause a state transition
        appGraph.postEvent(AppEvents.LoginEvent)
    }) {
        Text("Login now!")
    }
}

@Composable
fun Home() {
    Button(onClick = {
        // transition directly to the login screen, don't use an event
        appGraph.postTransition(AppState.Login)
    }) {
        Text("Log me out, please.")
    }
}