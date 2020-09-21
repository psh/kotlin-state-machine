package com.gatebuzz.statemachine.example.evenodd

import com.gatebuzz.statemachine.MachineState.Dwelling
import com.gatebuzz.statemachine.Node
import com.gatebuzz.statemachine.example.evenodd.MatterEvent.*
import com.gatebuzz.statemachine.example.evenodd.MatterState.*
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MatterStateMachineTest {
    @Before
    fun setUp() {
        logger = mock()
    }

    @Test
    fun `initial state should be solid`() {
        stateMachine.start()
        assertEquals(Solid, stateMachine.currentState.id)
    }

    @Test
    fun `melting should move us from solid to liquid`() {
        stateMachine.start(Dwelling(Node(Solid)))

        stateMachine.consume(OnMelted)

        assertEquals(Liquid, stateMachine.currentState.id)
        verify(logger).log(ON_MELTED_MESSAGE)
    }

    @Test
    fun `freezing should move us from liquid to solid`() {
        stateMachine.start(Dwelling(Node(Liquid)))

        stateMachine.consume(OnFrozen)

        assertEquals(Solid, stateMachine.currentState.id)
        verify(logger).log(ON_FROZEN_MESSAGE)
    }

    @Test
    fun `vaporization should move us from liquid to gas`() {
        stateMachine.start(Dwelling(Node(Liquid)))

        stateMachine.consume(OnVaporized)

        assertEquals(Gas, stateMachine.currentState.id)
        verify(logger).log(ON_VAPORIZED_MESSAGE)
    }

    @Test
    fun `condensation moves us from gas to liquid`() {
        stateMachine.start(Dwelling(Node(Gas)))

        stateMachine.consume(OnCondensed)

        assertEquals(Liquid, stateMachine.currentState.id)
        verify(logger).log(ON_CONDENSED_MESSAGE)
    }

    @Test
    fun `access the graph nodes`() {
        assertEquals(Node(Solid), stateMachine.findNode(Solid))
        assertEquals(Node(Liquid), stateMachine.findNode(Liquid))
        assertEquals(Node(Gas), stateMachine.findNode(Gas))
    }
}
