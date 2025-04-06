package earth.worldwind.shape.milstd2525

import earth.worldwind.render.Font
import earth.worldwind.shape.TextAttributes
import earth.worldwind.shape.milstd2525.renderer.MilStdIconRenderer
import earth.worldwind.shape.milstd2525.renderer.utilities.Color
import earth.worldwind.shape.milstd2525.renderer.utilities.ImageInfo
import earth.worldwind.shape.milstd2525.renderer.utilities.MilStdAttributes
import earth.worldwind.shape.milstd2525.renderer.utilities.ModifiersTG
import earth.worldwind.shape.milstd2525.renderer.utilities.ModifiersUnits
import earth.worldwind.shape.milstd2525.renderer.utilities.RendererSettings
import earth.worldwind.shape.milstd2525.renderer.utilities.RendererUtilities
import earth.worldwind.shape.milstd2525.renderer.utilities.SymbolDefTable
import earth.worldwind.shape.milstd2525.renderer.utilities.SymbolUtilities
import earth.worldwind.shape.milstd2525.renderer.utilities.UnitDefTable

/**
 * This utility class generates MIL-STD-2525 symbols and tactical graphics using the MIL-STD-2525 Symbol Rendering Library
 * @see <a href="https://github.com/missioncommand/mil-sym-js">https://github.com/missioncommand/mil-sym-js</a>
 */
actual object MilStd2525 {
    private const val GRAPHICS_LINE_WIDTH = 3f
    private const val SYMBOL_OUTLINE_WIDTH = 1

    /**
     * Controls the symbol modifiers visibility threshold
     */
    actual var modifiersThreshold = 3.2e4
    /**
     * Controls the tactical graphics labels visibility threshold
     */
    actual var labelScaleThreshold = 4.0
    actual var graphicsLineWidth = GRAPHICS_LINE_WIDTH
    var graphicsOutlineWidth = SYMBOL_OUTLINE_WIDTH.toFloat()

    init {
        // Initialize fonts
        RendererUtilities.fontsLoaded()

        // Initialize RendererSettings
        RendererSettings.setSymbologyStandard(RendererSettings.Symbology_2525C)

        // Depending on screen size and DPI you may want to change the font size.
        RendererSettings.setMPModifierFont("Arial", 12, "normal")
        RendererSettings.setModifierFont("Arial", 8, "normal")

        // Configure modifier text output
        RendererSettings.setTextBackgroundMethod(RendererSettings.TextBackgroundMethod_OUTLINE)
        RendererSettings.setTextOutlineWidth(1) // 2 is the factory default

        // Configure Single point symbol outline width
        RendererSettings.setSinglePointSymbolOutlineWidth(SYMBOL_OUTLINE_WIDTH)
    }

    /**
     * Creates an MIL-STD-2525 symbol from the specified symbol code, modifiers and attributes.
     *
     * @param symbolCode The MIL-STD-2525 symbol code.
     * @param modifiers  The MIL-STD-2525 modifiers. If null, a default (empty) modifier list will be used.
     * @param attributes The MIL-STD-2525 attributes. If null, a default (empty) attribute list will be used.
     *
     * @return An ImageInfo object containing the symbol's image and metadata may be null
     */
    fun renderImage(symbolCode: String, modifiers: Map<String, String>?, attributes: Map<String, String>?): ImageInfo? {
        val params: dynamic = object{}
        modifiers?.forEach { params[it.key] = it.value }
        attributes?.forEach { params[it.key] = it.value }
        return MilStdIconRenderer.Render(symbolCode, params)
    }

    /**
     * Get symbol text description and hierarchy reference.
     */
    fun getSymbolDef(sidc: String): dynamic =
        SymbolDefTable.getSymbolDef(SymbolUtilities.getBasicSymbolID(sidc), RendererSettings.getSymbologyStandard())

    /**
     * Get symbol visual attributes like draw category, min points, max points, etc.
     */
    fun getUnitDef(sidc: String): dynamic =
        UnitDefTable.getUnitDef(SymbolUtilities.getBasicSymbolID(sidc), RendererSettings.getSymbologyStandard())

    fun applyTextAttributes(textAttributes: TextAttributes) = textAttributes.apply {
        font = Font(
            size = RendererSettings.getModifierFontSize().toInt(),
            family = RendererSettings.getModifierFontName(),
            weight = RendererSettings.getModifierFontStyle()
        )
        val foregroundColor = RendererSettings.getLabelForegroundColor() ?: Color(0, 0, 0)
        val backgroundColor = RendererSettings.getLabelBackgroundColor() ?: Color.getColorFromHexString(
            RendererUtilities.getIdealOutlineColor(foregroundColor.toHexString(withAlpha = false), forceRGB = true)
        )
        textColor.set(foregroundColor.toARGB().toInt())
        outlineColor.set(backgroundColor.toARGB().toInt())
        outlineWidth =  RendererSettings.getTextOutlineWidth().toFloat()
    }

    actual fun getSimplifiedSymbolID(symbolID: String) =
        setAffiliation(SymbolUtilities.getBasicSymbolID(symbolID), symbolID.substring(1, 2))

    actual fun isTacticalGraphic(symbolID: String) = SymbolUtilities.isTacticalGraphic(symbolID)

    actual fun setAffiliation(symbolID: String, affiliation: String?) =
        // Weather symbols has no affiliation
        if (symbolID.length >= 2 && !SymbolUtilities.isWeather(symbolID) && affiliation != null && affiliation.length == 1) {
            val result = symbolID.substring(0, 1) + affiliation.uppercase() + symbolID.substring(2)
            if (SymbolUtilities.hasValidAffiliation(result)) result else symbolID
        } else symbolID

    actual fun setStatus(symbolID: String, status: String?) =
        // Weather symbols has no status
        if (symbolID.length >= 4 && !SymbolUtilities.isWeather(symbolID) && status != null && status.length == 1) {
            val result = symbolID.substring(0, 3) + status.uppercase() + symbolID.substring(4)
            if (SymbolUtilities.hasValidStatus(result)) result else symbolID
        } else symbolID

    actual fun setEchelon(symbolID: String, echelon: String?): String {
        val isTG = SymbolUtilities.isTacticalGraphic(symbolID)
        return if (symbolID.length >= 12 && (isTG && SymbolUtilities.canSymbolHaveModifier(symbolID, ModifiersTG.B_ECHELON)
                    || !isTG && SymbolUtilities.canUnitHaveModifier(symbolID, ModifiersUnits.B_ECHELON))
            && echelon != null && echelon.length == 1 && SymbolUtilities.getEchelonText(echelon).isNotEmpty()
        ) symbolID.substring(0, 11) + echelon.uppercase() + symbolID.substring(12) else symbolID
    }

    actual fun setMobility(symbolID: String, mobility: String?): String {
        return if (symbolID.length >= 12 && !SymbolUtilities.isTacticalGraphic(symbolID) && mobility != null && mobility.length == 2
            && SymbolUtilities.canUnitHaveModifier(symbolID, ModifiersUnits.R_MOBILITY_INDICATOR)) {
            val sidcWithMobility = symbolID.substring(0, 10) + mobility.uppercase() + symbolID.substring(12)
            // Check if mobility is valid
            if (SymbolUtilities.isMobility(sidcWithMobility)) sidcWithMobility else symbolID
        } else symbolID
    }

    actual fun setHQTFD(
        symbolID: String, hq: Boolean, taskForce: Boolean, feintDummy: Boolean
    ): String {
        var result = symbolID
        if (result.length >= 11 && !SymbolUtilities.isTacticalGraphic(result)) {
            // Check if HQ, TaskForce, Feint or Dummy symbol modifiers are applicable
            val isHQ = hq && SymbolUtilities.canUnitHaveModifier(result, ModifiersUnits.S_HQ_STAFF_OR_OFFSET_INDICATOR)
            val isTaskForce = taskForce && SymbolUtilities.canUnitHaveModifier(result, ModifiersUnits.D_TASK_FORCE_INDICATOR)
            val isDummy = feintDummy && SymbolUtilities.canUnitHaveModifier(result, ModifiersUnits.AB_FEINT_DUMMY_INDICATOR)
            val modifier = when {
                !isDummy && isHQ && !isTaskForce -> 'A' // HEADQUARTERS
                !isDummy && isHQ && isTaskForce -> 'B' // TASK FORCE + HEADQUARTERS
                isDummy && isHQ && !isTaskForce -> 'C' // FEINT/DUMMY + HEADQUARTERS
                isDummy && isHQ && isTaskForce -> 'D' // FEINT/DUMMY + TASK FORCE + HEADQUARTERS
                !isDummy && !isHQ && isTaskForce -> 'E' // TASK FORCE
                isDummy && !isHQ && !isTaskForce -> 'F' // FEINT/DUMMY
                isDummy && !isHQ && isTaskForce -> 'G' // FEINT/DUMMY + TASK FORCE
                else -> null
            }
            // Apply symbol modifier
            if (modifier != null) result = result.substring(0, 10) + modifier + result.substring(11)
            // Fix Feint/Dummy modifier for installation
            if (isDummy && result.length >= 12 && SymbolUtilities.hasInstallationModifier(result)) {
                result = result.substring(0, 11) + "B" + result.substring(12)
            }
        }
        return result
    }

    actual fun getLineColor(symbolID: String) = SymbolUtilities.getLineColorOfAffiliation(symbolID)?.toARGB()?.toInt()
        ?: RendererSettings.getFriendlyGraphicLineColor().toARGB().toInt()

    actual fun getFillColor(symbolID: String) = SymbolUtilities.getFillColorOfAffiliation(symbolID)?.toARGB()?.toInt()
        ?: RendererSettings.getFriendlyGraphicFillColor().toARGB().toInt()

    actual fun getUnfilledAttributes(symbolID: String) = if (isTacticalGraphic(symbolID)) {
        SymbolUtilities.getLineColorOfAffiliation(symbolID)
    } else {
        SymbolUtilities.getFillColorOfAffiliation(symbolID)
    }?.toHexString(true)?.let {
        mapOf(
            MilStdAttributes.FillColor to "00000000",
            MilStdAttributes.LineColor to it,
            MilStdAttributes.IconColor to it
        )
    } ?: emptyMap()
}
