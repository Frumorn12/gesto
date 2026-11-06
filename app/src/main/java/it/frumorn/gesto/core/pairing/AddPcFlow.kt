package it.frumorn.gesto.core.pairing

import it.frumorn.gesto.AddPcStep
import it.frumorn.gesto.R
import it.frumorn.gesto.core.errors.ErrorContext
import it.frumorn.gesto.core.errors.ErrorTranslator
import it.frumorn.gesto.core.model.DiscoveredHost
import it.frumorn.gesto.core.model.NoticeAction
import it.frumorn.gesto.core.network.GestoApiException
import it.frumorn.gesto.core.network.identity
import it.frumorn.gesto.ui.text.UiText

/** Where "Add a PC" goes after a pairing attempt. */
sealed interface AddPcNext {
    /** The PC let the phone in: the session opens. */
    data object Paired : AddPcNext

    /** Show [step], with [notice] (and its [action]) when there is something to say. */
    data class Step(val step: AddPcStep, val notice: UiText?, val action: NoticeAction? = null) : AddPcNext
}

/** The rules of "Add a PC" that do not need Android: first step, what follows a failure, the search list. */
object AddPcFlow {
    private const val DEFAULT_PORT = 47771

    /** A daemon that can ask on the PC gets the PIN; an older one the 6-digit code. */
    fun firstStepFor(host: DiscoveredHost): AddPcStep =
        if (host.approvalPairing) AddPcStep.Approval else AddPcStep.Code

    fun afterApproval(result: Result<*>, port: Int = DEFAULT_PORT): AddPcNext =
        result.fold(onSuccess = { AddPcNext.Paired }, onFailure = { afterApprovalFailure(it, port) })

    /**
     * After the PC did not let the phone in: declined, busy or not answering goes back to the search
     * (nobody answering also offers the code); a PC that cannot show the request, or a daemon too old
     * to know it (the connection ends at once, see `ApprovalNotSupportedException`), goes to the code.
     */
    fun afterApprovalFailure(error: Throwable, port: Int = DEFAULT_PORT): AddPcNext {
        val chain = generateSequence(error) { it.cause }.take(8).toList()
        val api = chain.firstOrNull { it is GestoApiException } as? GestoApiException
        return when {
            api?.code == "pairing_rejected" -> AddPcNext.Step(AddPcStep.Search, UiText.Res(R.string.error_rejected_on_pc))
            api?.code == "pairing_timeout" ->
                AddPcNext.Step(AddPcStep.Search, UiText.Res(R.string.error_no_answer), NoticeAction.UseCode)
            api?.code == "approval_unavailable" ->
                AddPcNext.Step(AddPcStep.Code, UiText.Res(R.string.error_approval_unavailable))
            api?.code == "pairing_busy" -> AddPcNext.Step(AddPcStep.Search, UiText.Res(R.string.error_pairing_busy))
            api == null && chain.any { it is java.io.EOFException } -> AddPcNext.Step(AddPcStep.Code, null)
            else -> AddPcNext.Step(AddPcStep.Search, ErrorTranslator.translate(error, ErrorContext.Pairing, port))
        }
    }

    /**
     * The PCs listed while searching: those that answered this [scan] or the [previousScan] (one lost
     * reply does not make a PC vanish, two do), each with its latest reply, in the order they first
     * appeared in [shown] so the list does not jump around between scans.
     */
    fun searchResults(
        shown: List<DiscoveredHost>,
        previousScan: List<DiscoveredHost>,
        scan: List<DiscoveredHost>,
    ): List<DiscoveredHost> {
        val latest = LinkedHashMap<String, DiscoveredHost>()
        (scan + previousScan).forEach { host -> latest.putIfAbsent(host.identity, host) }
        val kept = shown.mapNotNull { latest[it.identity] }.distinctBy { it.identity }
        val keptIds = kept.mapTo(HashSet()) { it.identity }
        return kept + latest.values.filter { it.identity !in keptIds }
    }
}
