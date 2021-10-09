package com.gatebuzz.statemachine.example.matter

import com.gatebuzz.statemachine.MachineState.Dwelling
import com.gatebuzz.statemachine.example.matter.MatterEvent.*
import com.gatebuzz.statemachine.example.matter.MatterState.*
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MatterStateMachineTest {
    @Before
    fun setUp() {
        logger = mockk(relaxed = true)
    }

    @Test
    fun `initial state should be solid`() {
        stateMachine.start()
        assertEquals(Solid, stateMachine.currentState.id)
    }

    @Test
    fun `melting should move us from solid to liquid`() {
        stateMachine.start(Dwelling(Solid))

        stateMachine.consume(OnMelted)

        assertEquals(Liquid, stateMachine.currentState.id)
        verify { logger.log(ON_MELTED_MESSAGE) }
    }

    @Test
    fun `freezing should move us from liquid to solid`() {
        stateMachine.start(Dwelling(Liquid))

        stateMachine.consume(OnFrozen)

        assertEquals(Solid, stateMachine.currentState.id)
        verify { logger.log(ON_FROZEN_MESSAGE) }
    }

    @Test
    fun `vaporization should move us from liquid to gas`() {
        stateMachine.start(Dwelling(Liquid))

        stateMachine.consume(OnVaporized)

        assertEquals(Gas, stateMachine.currentState.id)
        verify { logger.log(ON_VAPORIZED_MESSAGE) }
    }

    @Test
    fun `condensation moves us from gas to liquid`() {
        stateMachine.start(Dwelling(Gas))

        stateMachine.consume(OnCondensed)

        assertEquals(Liquid, stateMachine.currentState.id)
        verify { logger.log(ON_CONDENSED_MESSAGE) }
    }
}
