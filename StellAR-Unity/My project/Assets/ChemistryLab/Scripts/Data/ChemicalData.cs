using UnityEngine;

namespace ChemistryLab.Data
{
    /// <summary>
    /// Physical state of a chemical at room temperature
    /// </summary>
    public enum PhysicalState
    {
        Solid,
        Liquid,
        Gas,
        Aqueous // Dissolved in water
    }

    /// <summary>
    /// Category of chemical for organization
    /// </summary>
    public enum ChemicalCategory
    {
        Acid,
        Base,
        Salt,
        Metal,
        Indicator,
        Solvent,
        Oxidizer,
        Organic,
        Other
    }

    /// <summary>
    /// ScriptableObject that defines properties of a chemical substance
    /// </summary>
    [CreateAssetMenu(fileName = "NewChemical", menuName = "Chemistry Lab/Chemical Data")]
    public class ChemicalData : ScriptableObject
    {
        [Header("Basic Information")]
        [Tooltip("Display name of the chemical")]
        public string chemicalName = "Unknown Chemical";
        
        [Tooltip("Chemical formula (e.g., H2O, NaCl)")]
        public string formula = "";
        
        [Tooltip("Molecular weight in g/mol")]
        public float molecularWeight = 0f;
        
        [Tooltip("Category for organization")]
        public ChemicalCategory category = ChemicalCategory.Other;

        [Header("Physical Properties")]
        [Tooltip("Physical state at room temperature")]
        public PhysicalState state = PhysicalState.Liquid;
        
        [Tooltip("Color when in solution or as pure substance")]
        public Color color = Color.clear;
        
        [Tooltip("Is the chemical transparent/translucent?")]
        public bool isTransparent = true;
        
        [Tooltip("pH value (0-14, 7 is neutral). Only applicable for aqueous solutions")]
        [Range(0f, 14f)]
        public float pH = 7f;

        [Header("Flame Test")]
        [Tooltip("Does this chemical produce a colored flame?")]
        public bool hasFlameColor = false;
        
        [Tooltip("Color of flame when heated")]
        public Color flameColor = Color.yellow;

        [Header("Safety Information")]
        [Tooltip("Brief description of hazards")]
        [TextArea(2, 4)]
        public string hazardInfo = "";
        
        [Tooltip("Is this chemical corrosive?")]
        public bool isCorrosive = false;
        
        [Tooltip("Is this chemical flammable?")]
        public bool isFlammable = false;
        
        [Tooltip("Does it react violently with water?")]
        public bool reactsWithWater = false;

        [Header("Visual Settings")]
        [Tooltip("Icon to display in UI")]
        public Sprite icon;
        
        [Tooltip("Particle effect when this chemical reacts")]
        public GameObject reactionEffectPrefab;

        /// <summary>
        /// Get a formatted display string for this chemical
        /// </summary>
        public string GetDisplayName()
        {
            if (string.IsNullOrEmpty(formula))
                return chemicalName;
            return $"{chemicalName} ({formula})";
        }

        /// <summary>
        /// Check if this chemical is acidic (pH < 7)
        /// </summary>
        public bool IsAcidic => pH < 7f;

        /// <summary>
        /// Check if this chemical is basic/alkaline (pH > 7)
        /// </summary>
        public bool IsBasic => pH > 7f;

        /// <summary>
        /// Check if this chemical is neutral (pH = 7)
        /// </summary>
        public bool IsNeutral => Mathf.Approximately(pH, 7f);
    }
}
