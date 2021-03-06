package fi.thl.koronahaavi.diagnosis

import android.os.Bundle
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.ChoiceFragment
import fi.thl.koronahaavi.common.ChoiceData.Choice

@AndroidEntryPoint
class ShareConsentFragment : ChoiceFragment() {
    private val viewModel: CodeEntryViewModel by hiltNavGraphViewModels(R.id.diagnosis_share_navigation)

    private val args by navArgs<ShareConsentFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            viewModel.code.value = args.code
        }
    }

    override fun getChoiceViewModel() = viewModel.shareData.consentChoice

    override val headerTextId = R.string.share_consent_header
    override val bodyTextId: Int? = null
    override val firstChoiceTextId = R.string.share_consent_eu
    override val secondChoiceTextId = R.string.share_consent_finland

    override val footerTextId: Int?
        get() = if (args.code == null) null else R.string.share_consent_code_saved

    override fun getNextDirections(choice: Choice) =
            if (choice == Choice.FIRST && viewModel.shareData.isTravelSelectionAvailable())
                // need to skip travel, if country selection data not available for some reason
                ShareConsentFragmentDirections.toTravelDisclosure()
            else
                ShareConsentFragmentDirections.toSummaryConsent()
}