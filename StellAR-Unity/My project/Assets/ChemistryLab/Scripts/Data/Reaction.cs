using UnityEngine;
using System.Collections.Generic;

namespace ChemistryLab.Data
{
    /// <summary>
    /// Represents the visual effect type for a reaction
    /// </summary>
    public enum ReactionEffectType
    {
        None,
        ColorChange,
        Precipitate,      // Solid forms and falls
        GasEvolution,     // Bubbles/vapor released
        Heat,             // Exothermic reaction
        Flame,            // Fire/combustion
        Explosion,        // Violent reaction
        Dissolve,         // Solid dissolves
        Fizzing,          // Effervescence
        Foam,             // Massive foam expansion (Elephant toothpaste)
        Smoke             // Smoke evolution
    }

    /// <summary>
    /// Represents a single reactant or product in a reaction
    /// </summary>
    [System.Serializable]
    public class ReactionComponent
    {
        public ChemicalData chemical;
        [Tooltip("Stoichiometric coefficient (e.g., 2 for 2HCl)")]
        public int coefficient = 1;
        [Tooltip("Amount in moles")]
        public float moles = 1f;
    }

    /// <summary>
    /// ScriptableObject that defines a chemical reaction
    /// </summary>
    [CreateAssetMenu(fileName = "NewReaction", menuName = "Chemistry Lab/Reaction")]
    public class Reaction : ScriptableObject
    {
        [Header("Reaction Information")]
        [Tooltip("Name of this reaction")]
        public string reactionName = "New Reaction";
        
        [Tooltip("Balanced chemical equation as text")]
        public string equationText = "";
        
        [TextArea(2, 4)]
        [Tooltip("Description of what happens in this reaction")]
        public string description = "";

        [Header("Reactants")]
        [Tooltip("Chemicals required for this reaction")]
        public List<ReactionComponent> reactants = new List<ReactionComponent>();

        [Header("Products")]
        [Tooltip("Chemicals produced by this reaction")]
        public List<ReactionComponent> products = new List<ReactionComponent>();

        [Header("Conditions")]
        [Tooltip("Minimum temperature (Celsius) required for reaction")]
        public float minTemperature = 20f;
        
        [Tooltip("Maximum temperature before reaction becomes dangerous")]
        public float maxTemperature = 100f;
        
        [Tooltip("Does this reaction require heating?")]
        public bool requiresHeat = false;
        
        [Tooltip("Optional catalyst required")]
        public ChemicalData catalyst;

        [Header("Effects")]
        [Tooltip("Primary visual effect of this reaction")]
        public ReactionEffectType primaryEffect = ReactionEffectType.None;
        
        [Tooltip("Secondary effects")]
        public List<ReactionEffectType> secondaryEffects = new List<ReactionEffectType>();
        
        [Tooltip("Color of precipitate if applicable")]
        public Color precipitateColor = Color.white;
        
        [Tooltip("Is this an exothermic (heat-releasing) reaction?")]
        public bool isExothermic = false;
        
        [Tooltip("Heat released/absorbed in kJ/mol")]
        public float enthalpyChange = 0f;

        [Header("Audio")]
        [Tooltip("Sound effect when reaction occurs")]
        public AudioClip reactionSound;

        /// <summary>
        /// Check if two chemicals can react according to this reaction
        /// </summary>
        public bool CanReact(ChemicalData chemical1, ChemicalData chemical2)
        {
            if (reactants.Count < 2) return false;
            
            bool hasFirst = false;
            bool hasSecond = false;
            
            foreach (var reactant in reactants)
            {
                if (reactant.chemical == chemical1) hasFirst = true;
                if (reactant.chemical == chemical2) hasSecond = true;
            }
            
            return hasFirst && hasSecond;
        }

        /// <summary>
        /// Check if all required reactants are present in the given list
        /// </summary>
        public bool HasAllReactants(List<ChemicalData> chemicals)
        {
            foreach (var reactant in reactants)
            {
                if (!chemicals.Contains(reactant.chemical))
                    return false;
            }
            return true;
        }

        /// <summary>
        /// Get the formatted equation string
        /// </summary>
        public string GetEquation()
        {
            if (!string.IsNullOrEmpty(equationText))
                return equationText;
            
            // Auto-generate from reactants and products
            string eq = "";
            for (int i = 0; i < reactants.Count; i++)
            {
                if (i > 0) eq += " + ";
                if (reactants[i].coefficient > 1)
                    eq += reactants[i].coefficient.ToString();
                eq += reactants[i].chemical?.formula ?? "?";
            }
            eq += " → ";
            for (int i = 0; i < products.Count; i++)
            {
                if (i > 0) eq += " + ";
                if (products[i].coefficient > 1)
                    eq += products[i].coefficient.ToString();
                eq += products[i].chemical?.formula ?? "?";
            }
            return eq;
        }
    }
}
