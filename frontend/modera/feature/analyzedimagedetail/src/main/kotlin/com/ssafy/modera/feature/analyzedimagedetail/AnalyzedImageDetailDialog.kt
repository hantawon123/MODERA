package com.ssafy.modera.feature.analyzedimagedetail

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.ssafy.modera.core.designsystem.icon.ModeraIcons

internal enum class AnalyzedImageDetailDialog(
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    @param:StringRes val confirmTextRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    REANALYZE(
        titleRes = R.string.analyzed_image_detail_reanalyze_dialog_title,
        descriptionRes =
            R.string.analyzed_image_detail_reanalyze_dialog_description,
        confirmTextRes =
            R.string.analyzed_image_detail_reanalyze_dialog_confirm,
        iconRes = ModeraIcons.Refresh,
    ),
    DELETE(
        titleRes = R.string.analyzed_image_detail_delete_dialog_title,
        descriptionRes =
            R.string.analyzed_image_detail_delete_dialog_description,
        confirmTextRes =
            R.string.analyzed_image_detail_delete_dialog_confirm,
        iconRes = ModeraIcons.Trash,
    ),
}