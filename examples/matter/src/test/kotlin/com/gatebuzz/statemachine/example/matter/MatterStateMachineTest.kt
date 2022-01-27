package com.gatebuzz.statemachine.example.matter

import com.gatebuzz.statemachine.MachineState.Dwelling
import com.gatebuzz.statemachine.example.matter.MatterEvent.*
import com.gatebuzz.statemachine.example.matter.MatterState.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@ExperimentalCoroutinesApi
class MatterStateMachineTest {
    @Test
    fun `initial state should be solid`() = runTest {
        stateMachine.start()
        assertEquals(Solid, stateMachine.currentState.id)
    }

    @Test
    fun `melting should move us from solid to liquid`() = runTest {
        stateMachine.start(Dwelling(Solid))

        stateMachine.consume(OnMelted)

        assertEquals(Liquid, stateMachine.currentState.id)
        assertEquals(ON_MELTED_MESSAGE, TestLogger.latest)
    }

    @Test
    fun `freezing should move us from liquid to solid`() = runTest {
        stateMachine.start(Dwelling(Liquid))

        stateMachine.consume(OnFrozen)

        assertEquals(Solid, stateMachine.currentState.id)
        assertEquals(ON_FROZEN_MESSAGE, TestLogger.latest)
    }

    @Test
    fun `vaporization should move us from liquid to gas`() = runTest {
        stateMachine.start(Dwelling(Liquid))

        stateMachine.consume(OnVaporized)

        assertEquals(Gas, stateMachine.currentState.id)
        assertEquals(ON_VAPORIZED_MESSAGE, TestLogger.latest)
    }

    @Test
    fun `condensation moves us from gas to liquid`() = runTest {
        stateMachine.start(Dwelling(Gas))

        stateMachine.consume(OnCondensed)

        assertEquals(Liquid, stateMachine.currentState.id)
        assertEquals(ON_CONDENSED_MESSAGE, TestLogger.latest)
    }
}
