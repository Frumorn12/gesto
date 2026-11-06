package it.frumorn.gesto.core.pairing

import it.frumorn.gesto.AddPcStep
import it.frumorn.gesto.R
import it.frumorn.gesto.core.errors.ErrorContext
import it.frumorn.gesto.core.errors.ErrorTranslator
import it.frumorn.gesto.core.model.DiscoveredHost
import it.frumorn.gesto.core.model.NoticeAction
import it.frumorn.gesto.core.network.ApprovalNotSupportedException
import it.frumorn.gesto.core.network.GestoApiException
import it.frumorn.gesto.ui.text.UiText
import java.io.EOFException
import java.net.ConnectException
import org.junit.Assert.assertEquals
import org.junit.Test

class AddPcFlowTest {
    private fun host(
        name: String = "legion",
        fingerprint: String = "fp-$name",
        address: String = "192.168.0.22",
        approval: Boolean = false,
    ) = DiscoveredHost(
        hostName = name,
        address = address,
        port = 47771,
        fingerprint = fingerprint,
        inputSupported = true,
        inputBackend = "portal",
        approvalPairing = approval,
    )

    private fun api(code: String) = GestoApiException(code, "msg", false)
    private fun step(step: AddPcStep, text: Int?, action: NoticeAction? = null) =
        AddPcNext.Step(step, text?.let { UiText.Res(it) }, action)

    @Test fun a_daemon_that_can_ask_on_the_pc_starts_with_the_pin() {
        assertEquals(AddPcStep.Approval, AddPcFlow.firstStepFor(host(approval = true)))
    }

    @Test fun an_old_daemon_starts_with_the_code() {
        assertEquals(AddPcStep.Code, AddPcFlow.firstStepFor(host(approval = false)))
    }

    @Test fun declined_on_the_pc_goes_back_to_search() {
        assertEquals(
            step(AddPcStep.Search, R.string.error_rejected_on_pc),
            AddPcFlow.afterApprovalFailure(api("pairing_rejected")),
        )
    }

    @Test fun nobody_answering_offers_the_code() {
        assertEquals(
            step(AddPcStep.Search, R.string.error_no_answer, NoticeAction.UseCode),
            AddPcFlow.afterApprovalFailure(api("pairing_timeout")),
        )
    }

    @Test fun a_pc_without_notifications_goes_to_the_code() {
        assertEquals(
            step(AddPcStep.Code, R.string.error_approval_unavailable),
            AddPcFlow.afterApprovalFailure(api("approval_unavailable")),
        )
    }

    @Test fun a_busy_pc_goes_back_to_search() {
        assertEquals(
            step(AddPcStep.Search, R.string.error_pairing_busy),
            AddPcFlow.afterApprovalFailure(api("pairing_busy")),
        )
    }

    @Test fun an_old_daemon_goes_to_the_code_without_a_message() {
        assertEquals(step(AddPcStep.Code, null), AddPcFlow.afterApprovalFailure(EOFException()))
        assertEquals(
            step(AddPcStep.Code, null),
            AddPcFlow.afterApprovalFailure(ApprovalNotSupportedException(IllegalStateException("Connection closed"))),
        )
    }

    @Test fun anything_else_is_translated_and_goes_back_to_search() {
        val refused = ConnectException("refused")
        assertEquals(
            AddPcNext.Step(AddPcStep.Search, ErrorTranslator.translate(refused, ErrorContext.Pairing)),
            AddPcFlow.afterApprovalFailure(refused),
        )
        assertEquals(
            AddPcNext.Step(AddPcStep.Search, UiText.Res(R.string.error_pc_refused, listOf(47800))),
            AddPcFlow.afterApprovalFailure(refused, port = 47800),
        )
        val limited = api("rate_limited")
        assertEquals(
            AddPcNext.Step(AddPcStep.Search, UiText.Res(R.string.error_too_many_attempts)),
            AddPcFlow.afterApprovalFailure(limited),
        )
    }

    @Test fun a_success_is_paired() {
        assertEquals(AddPcNext.Paired, AddPcFlow.afterApproval(Result.success(Unit)))
        assertEquals(
            step(AddPcStep.Search, R.string.error_rejected_on_pc),
            AddPcFlow.afterApproval(Result.failure<Unit>(api("pairing_rejected"))),
        )
    }

    // The list on the search step.

    @Test fun search_lists_new_pcs_in_the_order_they_answered() {
        val a = host("a")
        val b = host("b")
        assertEquals(listOf(a, b), AddPcFlow.searchResults(emptyList(), emptyList(), listOf(a, b)))
    }

    @Test fun search_keeps_the_order_when_replies_arrive_in_another_order() {
        val a = host("a")
        val b = host("b")
        assertEquals(listOf(a, b), AddPcFlow.searchResults(listOf(a, b), listOf(a, b), listOf(b, a)))
    }

    @Test fun search_keeps_a_pc_missed_once_and_drops_it_after_two_misses() {
        val a = host("a")
        val b = host("b")
        val once = AddPcFlow.searchResults(listOf(a, b), listOf(a, b), listOf(a))
        assertEquals(listOf(a, b), once)
        assertEquals(listOf(a), AddPcFlow.searchResults(once, listOf(a), listOf(a)))
    }

    @Test fun search_shows_the_latest_reply_of_a_pc() {
        val old = host("a", address = "192.168.0.10")
        val moved = host("a", address = "192.168.0.11")
        assertEquals(listOf(moved), AddPcFlow.searchResults(listOf(old), listOf(old), listOf(moved)))
    }

    @Test fun search_tells_pcs_without_fingerprint_apart_by_address() {
        val a = host("a", fingerprint = "", address = "192.168.0.10")
        val b = host("b", fingerprint = "", address = "192.168.0.11")
        assertEquals(listOf(a, b), AddPcFlow.searchResults(emptyList(), emptyList(), listOf(a, b, a)))
    }
}
