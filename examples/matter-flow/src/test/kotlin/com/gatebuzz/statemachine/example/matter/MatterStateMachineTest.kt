package com.gatebuzz.statemachine.example.matter

import app.cash.turbine.test
import com.gatebuzz.statemachine.MachineState.Dwelling
import com.gatebuzz.statemachine.MachineState.Traversing
import com.gatebuzz.statemachine.example.matter.MatterEvent.*
import com.gatebuzz.statemachine.example.matter.MatterState.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
@ExperimentalTime
class MatterStateMachineTest {
    @Test
    fun `initial state should be solid`() = runTest {
        stateMachine.start()
        assertEquals(Solid, stateMachine.currentState.id)
    }

    @Test
    fun `melting should move us from solid to liquid`() = runTest {
        stateMachine.start(Dwelling(Solid))

        stateMachine.observeStateChanges().test {
            stateMachine.consume(OnMelted)

            assertEquals(Traversing(edge = Solid to Liquid, trigger = OnMelted), awaitItem())
            assertEquals(Dwelling(Liquid), awaitItem())

            assertEquals(Liquid, stateMachine.currentState.id)
        }
    }

    @Test
    fun `freezing should move us from liquid to solid`() = runTest {
        stateMachine.start(Dwelling(Liquid))

        stateMachine.observeStateChanges().test {
            stateMachine.consume(OnFrozen)

            assertEquals(Traversing(edge = Liquid to Solid, trigger = OnFrozen), awaitItem())
            assertEquals(Dwelling(Solid), awaitItem())

            assertEquals(Solid, stateMachine.currentState.id)
        }
    }

    @Test
    fun `vaporization should move us from liquid to gas`() = runTest {
        stateMachine.start(Dwelling(Liquid))

        stateMachine.observeStateChanges().test {
            stateMachine.consume(OnVaporized)

            assertEquals(Traversing(edge = Liquid to Gas, trigger = OnVaporized), awaitItem())
            assertEquals(Dwelling(Gas), awaitItem())

            assertEquals(Gas, stateMachine.currentState.id)
        }
    }

    @Test
    fun `condensation moves us from gas to liquid`() = runTest {
        stateMachine.start(Dwelling(Gas))

        stateMachine.observeStateChanges().test {
            stateMachine.consume(OnCondensed)

            assertEquals(Traversing(edge = Gas to Liquid, trigger = OnCondensed), awaitItem())
            assertEquals(Dwelling(Liquid), awaitItem())

            assertEquals(Liquid, stateMachine.currentState.id)
        }
    }
}
