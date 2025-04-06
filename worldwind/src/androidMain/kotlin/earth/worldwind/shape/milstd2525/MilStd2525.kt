package earth.worldwind.shape.milstd2525

import android.content.Context
import android.graphics.Typeface
import armyc2.c5isr.renderer.MilStdIconRenderer
import armyc2.c5isr.renderer.utilities.*
import earth.worldwind.R
import earth.worldwind.render.Font
import earth.worldwind.shape.TextAttributes

/**
 * This utility class generates MIL-STD-2525 symbols and tactical graphics using the MIL-STD-2525 Symbol Rendering Library
 * @see <a href="https://github.com/missioncommand/mil-sym-android">https://github.com/missioncommand/mil-sym-android</a>
 */
actual object MilStd2525 {
    /**
     * Controls the symbol modifiers visibility threshold
     */
    actual var modifiersThreshold = 3.2e4
    /**
     * Controls the tactical graphics labels visibility threshold
     */
    actual var labelScaleThreshold = 30.0
    actual var graphicsLineWidth = 0f
    var graphicsOutlineWidth = 0f
    @JvmStatic
    var isInitialized = false
        private set
    /**
     * The actual rendering engine for the MIL-STD-2525 graphics.
     */
    private val renderer = MilStdIconRenderer.getInstance()
    private val rendererSettings = RendererSettings.getInstance()
    private val msLookup = MSLookup.getInstance()

    /**
     * Initializes the static MIL-STD-2525 symbol renderer.  This method must be called one time before calling
     * renderImage().
     *
     * @param context The Context used to define the location of the renderer's cache directly.
     */
    @JvmStatic
    @Synchronized
    fun initializeRenderer(context: Context) {
        if (isInitialized) return

        // Establish the default rendering values.
        rendererSettings.defaultPixelSize = context.resources.getDimensionPixelSize(R.dimen.default_pixel_size)

        // Depending on screen size and DPI you may want to change the font size.
        rendererSettings.setModifierFont("Arial", Typeface.BOLD, context.resources.getDimensionPixelSize(R.dimen.modifier_font_size))
        rendererSettings.setMPLabelFont("Arial", Typeface.BOLD, context.resources.getDimensionPixelSize(R.dimen.mp_modifier_font_size))

        // Configure modifier text output
        rendererSettings.textBackgroundMethod = RendererSettings.TextBackgroundMethod_OUTLINE

        // Tell the renderer where the cache folder is located which is needed to process the embedded xml files.
        renderer.init(context)
        msLookup.init(context)
        graphicsLineWidth = context.resources.getDimension(R.dimen.graphics_line_width)
        graphicsOutlineWidth = context.resources.getDimension(R.dimen.graphics_outline_width)
        isInitialized = true
    }

    /**
     * Creates an MIL-STD-2525 symbol from the specified symbol code, modifiers and attributes.
     *
     * @param symbolCode The MIL-STD-2525 symbol code.
     * @param modifiers  The MIL-STD-2525 modifiers. If null, a default (empty) modifier list will be used.
     * @param attributes The MIL-STD-2525 attributes. If null, a default (empty) attribute list will be used.
     *
     * @return An ImageInfo object containing the symbol's bitmap and metadata; may be null
     */
    @JvmStatic
    fun renderImage(
        symbolCode: String, modifiers: Map<String, String>?, attributes: Map<String, String>?
    ): ImageInfo? = renderer.RenderIcon(
        symbolCode, modifiers?.map { Modifiers.getModifierKey(it.key) to it.value }?.toMap() ?: emptyMap(), attributes ?: emptyMap()
    )

    /**
     * Get symbol text description and visual attributes like draw category, min points, max points, etc.
     */
    @JvmStatic
    fun getMSLInfo(symbolID: String): MSInfo? = msLookup.getMSLInfo(symbolID)

    @JvmStatic
    fun applyTextAttributes(textAttributes: TextAttributes) = textAttributes.apply {
        val modifierFont = rendererSettings.modiferFont
        font = Font(modifierFont.textSize, modifierFont.typeface)
//        textColor.set(rendererSettings.labelForegroundColor)
//        outlineColor.set(rendererSettings.labelBackgroundColor)
//        outlineWidth = rendererSettings.textOutlineWidth.toFloat()
    }

    @JvmStatic
    actual fun getSimplifiedSymbolID(symbolID: String) = symbolID.substring(0, 6) + "0000" + symbolID.substring(10, 16) + "0000"

    @JvmStatic
    actual fun isTacticalGraphic(symbolID: String) = SymbolUtilities.isTacticalGraphic(symbolID)

    @JvmStatic
    actual fun setAffiliation(symbolID: String, affiliation: String?) = affiliation?.toIntOrNull()?.let {
        SymbolID.setAffiliation(symbolID, it)
    } ?: symbolID

    @JvmStatic
    actual fun setStatus(symbolID: String, status: String?) = status?.toIntOrNull()?.let {
        SymbolID.setStatus(symbolID, it)
    } ?: symbolID

    @JvmStatic
    actual fun setEchelon(symbolID: String, echelon: String?) = echelon?.toIntOrNull()?.let {
        SymbolID.setAmplifierDescriptor(symbolID, it)
    } ?: symbolID

    @JvmStatic
    actual fun setMobility(symbolID: String, mobility: String?) = mobility?.toIntOrNull()?.let {
        SymbolID.setAmplifierDescriptor(symbolID, it)
    } ?: symbolID

    @JvmStatic
    actual fun setHQTFD(symbolID: String, hq: Boolean, taskForce: Boolean, feintDummy: Boolean): String {
        val isHQ = hq && SymbolUtilities.isHQ(symbolID)
        val isTaskForce = taskForce && SymbolUtilities.isTaskForce(symbolID)
        val isDummy = feintDummy && SymbolUtilities.hasModifier(symbolID, Modifiers.AB_FEINT_DUMMY_INDICATOR)
        val HQTFD = when {
            isDummy && !isHQ && !isTaskForce -> 1 // FEINT/DUMMY
            !isDummy && isHQ && !isTaskForce -> 2 // HEADQUARTERS
            isDummy && isHQ && !isTaskForce -> 3 // FEINT/DUMMY + HEADQUARTERS
            !isDummy && !isHQ && isTaskForce -> 4 // TASK FORCE
            isDummy && !isHQ && isTaskForce -> 5 // FEINT/DUMMY + TASK FORCE
            !isDummy && isHQ && isTaskForce -> 6 // TASK FORCE + HEADQUARTERS
            isDummy && isHQ && isTaskForce -> 7 // FEINT/DUMMY + TASK FORCE + HEADQUARTERS
            else -> 0
        }
        return SymbolID.setHQTFD(symbolID, HQTFD)
    }

    @JvmStatic
    actual fun getLineColor(symbolID: String) = SymbolUtilities.getLineColorOfAffiliation(symbolID)?.toARGB()
        ?: AffiliationColors.FriendlyGraphicLineColor.toARGB()

    @JvmStatic
    actual fun getFillColor(symbolID: String) = SymbolUtilities.getFillColorOfAffiliation(symbolID)?.toARGB()
        ?: AffiliationColors.FriendlyGraphicFillColor.toARGB()

    @JvmStatic
    actual fun getUnfilledAttributes(symbolID: String) = if (SymbolUtilities.isTacticalGraphic(symbolID)) {
        SymbolUtilities.getLineColorOfAffiliation(symbolID)
    } else {
        SymbolUtilities.getFillColorOfAffiliation(symbolID)
    }?.toHexString()?.let {
        mapOf(
            MilStdAttributes.FillColor.toString() to "00000000",
            MilStdAttributes.LineColor.toString() to it,
            MilStdAttributes.IconColor.toString() to it
        )
    } ?: emptyMap()
}