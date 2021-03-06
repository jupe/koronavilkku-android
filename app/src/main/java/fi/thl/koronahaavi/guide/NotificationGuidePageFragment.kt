package fi.thl.koronahaavi.guide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import fi.thl.koronahaavi.R
import fi.thl.koronahaavi.common.safeGetDrawable
import fi.thl.koronahaavi.common.safeGetString
import fi.thl.koronahaavi.databinding.FragmentNotificationGuidePageBinding

class NotificationGuidePageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNotificationGuidePageBinding.inflate(inflater, container, false)

        arguments?.let { args ->
            val pageNum = args.getInt(ARG_CURRENT_PAGE_ID)
            val pageCount = args.getInt(ARG_PAGE_COUNT_ID)
            binding.textNotificationGuidePageNumber.text = pageNum.toString()

            // set page number to content description to help with accessibility
            binding.textNotificationGuidePageNumber.contentDescription =
                getString(R.string.notification_guide_page_content_description, pageNum, pageCount)

            // since text and image resource id's are provided as arguments, use safe methods to load resources
            // to prevent errors from invalid or missing arguments
            if (args.containsKey(ARG_TITLE_TEXT_ID)) {
                binding.textNotificationGuidePageTitle.text = context?.safeGetString(args.getInt(ARG_TITLE_TEXT_ID))
            } else {
                binding.textNotificationGuidePageTitle.visibility = View.GONE
            }

            binding.textNotificationGuidePageBody.text = context?.safeGetString(args.getInt(ARG_BODY_TEXT_ID))

            binding.imageNotificationGuidePage.setImageDrawable(
                    context?.safeGetDrawable(args.getInt(ARG_IMAGE_ID))
            )
        }

        return binding.root
    }

    companion object {
        fun create(pageNum: Int,
                   pageCount: Int,
                   @StringRes titleTextResId: Int?,
                   @StringRes bodyTextResId: Int,
                   @DrawableRes imageResId: Int)
        = NotificationGuidePageFragment().apply {
                arguments = bundleOf(
                        ARG_CURRENT_PAGE_ID to pageNum,
                        ARG_PAGE_COUNT_ID to pageCount,
                        ARG_BODY_TEXT_ID to bodyTextResId,
                        ARG_IMAGE_ID to imageResId
                ).apply {
                    titleTextResId?.let { putInt(ARG_TITLE_TEXT_ID, it) }
                }
            }

        const val ARG_TITLE_TEXT_ID = "title_text_id"
        const val ARG_BODY_TEXT_ID = "body_text_id"
        const val ARG_IMAGE_ID = "image_id"
        const val ARG_CURRENT_PAGE_ID = "current_page_id"
        const val ARG_PAGE_COUNT_ID = "page_count_id"
    }
}