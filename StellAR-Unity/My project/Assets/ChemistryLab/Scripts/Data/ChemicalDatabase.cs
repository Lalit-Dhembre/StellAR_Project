using UnityEngine;
using System.Collections.Generic;
using System.Linq;

namespace ChemistryLab.Data
{
    /// <summary>
    /// Central database holding all chemicals and reactions.
    /// Singleton pattern for easy access throughout the project.
    /// </summary>
    [CreateAssetMenu(fileName = "ChemicalDatabase", menuName = "Chemistry Lab/Chemical Database")]
    public class ChemicalDatabase : ScriptableObject
    {
        [Header("Chemicals")]
        [Tooltip("All available chemicals in the lab")]
        public List<ChemicalData> chemicals = new List<ChemicalData>();

        [Header("Reactions")]
        [Tooltip("All possible reactions")]
        public List<Reaction> reactions = new List<Reaction>();

        // Cached dictionaries for fast lookup
        private Dictionary<string, ChemicalData> _chemicalsByFormula;
        private Dictionary<string, ChemicalData> _chemicalsByName;

        /// <summary>
        /// Initialize lookup dictionaries
        /// </summary>
        public void Initialize()
        {
            if (chemicals == null) chemicals = new List<ChemicalData>();
            if (reactions == null) reactions = new List<Reaction>();

            // Hardcode defaults if empty (Runtime fallback)
            if (chemicals.Count == 0)
            {
                PopulateDefaults();
            }

            _chemicalsByFormula = new Dictionary<string, ChemicalData>();
            _chemicalsByName = new Dictionary<string, ChemicalData>();

            foreach (var chemical in chemicals)
            {
                if (chemical == null) continue;
                
                if (!string.IsNullOrEmpty(chemical.formula))
                    _chemicalsByFormula[chemical.formula.ToLower()] = chemical;
                
                if (!string.IsNullOrEmpty(chemical.chemicalName))
                    _chemicalsByName[chemical.chemicalName.ToLower()] = chemical;
            }
        }

        private void PopulateDefaults()
        {
            Debug.Log("Populating FULL default Chemical Database");

            // --- Create Chemicals ---
            
            // Solvents
            var h2o = CreateConceptChemical("H2O", "Water", ChemicalCategory.Solvent, new Color(0.4f, 0.7f, 1f, 0.3f), 7f);
            
            // Acids
            var hcl = CreateConceptChemical("HCl", "Hydrochloric Acid", ChemicalCategory.Acid, new Color(0.95f, 0.9f, 0.7f, 0.1f), 1f); 
            
            // Bases
            var naoh = CreateConceptChemical("NaOH", "Sodium Hydroxide", ChemicalCategory.Base, new Color(0.85f, 0.9f, 1f, 0.1f), 14f);
            
            // Salts
            var nacl = CreateConceptChemical("NaCl", "Sodium Chloride", ChemicalCategory.Salt, new Color(0.95f, 0.95f, 0.9f, 0.1f), 7f);
            var agno3 = CreateConceptChemical("AgNO3", "Silver Nitrate", ChemicalCategory.Salt, new Color(0.9f, 0.9f, 0.95f, 0.1f), 7f);
            var cuso4 = CreateConceptChemical("CuSO4", "Copper Sulfate", ChemicalCategory.Salt, new Color(0.1f, 0.5f, 0.95f, 0.8f), 7f); // Blue
            var ki = CreateConceptChemical("KI", "Potassium Iodide", ChemicalCategory.Salt, Color.white, 7f);
            
            // Metals
            var na = CreateConceptChemical("Na", "Sodium", ChemicalCategory.Metal, new Color(0.8f, 0.8f, 0.85f, 1f), 7f);
            na.state = PhysicalState.Solid; na.isFlammable = true; na.reactsWithWater = true;
            
            var k = CreateConceptChemical("K", "Potassium", ChemicalCategory.Metal, new Color(0.75f, 0.75f, 0.8f, 1f), 7f);
            k.state = PhysicalState.Solid; k.isFlammable = true; k.reactsWithWater = true;

            // Oxidizers
            var h2o2 = CreateConceptChemical("H2O2", "Hydrogen Peroxide", ChemicalCategory.Oxidizer, new Color(0.95f, 0.95f, 1f, 0.1f), 6f);
            var kmno4 = CreateConceptChemical("KMnO4", "Potassium Permanganate", ChemicalCategory.Oxidizer, new Color(0.5f, 0f, 0.5f, 0.9f), 7f); // Purple
            
            // Organics / Others
            var glycerin = CreateConceptChemical("C3H8O3", "Glycerin", ChemicalCategory.Organic, new Color(1f, 0.95f, 0.9f, 0.3f), 7f);
            glycerin.isFlammable = true;
            
            var soap = CreateConceptChemical("Soap", "Dish Soap", ChemicalCategory.Other, new Color(0.2f, 0.8f, 0.4f, 0.6f), 8f);
            
            // Indicators
            var phenol = CreateConceptChemical("C20H14O4", "Phenolphthalein", ChemicalCategory.Indicator, new Color(1f, 1f, 1f, 0.05f), 7f);

            // New Chemicals for User Request
            var vinegar = CreateConceptChemical("CH3COOH", "Acetic Acid (Vinegar)", ChemicalCategory.Acid, new Color(1f, 0.9f, 0.8f, 0.2f), 3f);
            var bakingSoda = CreateConceptChemical("NaHCO3", "Baking Soda", ChemicalCategory.Base, Color.white, 8.5f);
            bakingSoda.state = PhysicalState.Solid;
            
            var ammonia = CreateConceptChemical("NH3", "Ammonia", ChemicalCategory.Base, new Color(1f, 1f, 1f, 0.1f), 11f);
            
            var mg = CreateConceptChemical("Mg", "Magnesium Strip", ChemicalCategory.Metal, new Color(0.8f, 0.8f, 0.85f, 1f), 7f);
            mg.state = PhysicalState.Solid; mg.isFlammable = true;

            // Add all to list
            chemicals.AddRange(new[] { h2o, hcl, naoh, nacl, agno3, cuso4, ki, na, k, h2o2, kmno4, glycerin, soap, phenol, vinegar, bakingSoda, ammonia, mg });

            // --- Create Reactions ---

            // 1. Neutralization
            reactions.Add(CreateConceptReaction("Neutralization", "HCl + NaOH → NaCl + H2O", 
                new[] { hcl, naoh }, new[] { nacl, h2o }, ReactionEffectType.Heat, true, enthalpy: -57.6f));

            // 2. Silver Chloride Precipitation (White)
            reactions.Add(CreateConceptReaction("AgCl Precipitation", "AgNO3 + NaCl → AgCl↓ + NaNO3", 
                new[] { agno3, nacl }, new[] { nacl }, ReactionEffectType.Precipitate, false, precipitateColor: Color.white));

            // 3. Copper Hydroxide Precipitation (Blue)
            reactions.Add(CreateConceptReaction("Cu(OH)2 Formation", "CuSO4 + 2NaOH → Cu(OH)2↓ + Na2SO4",
                new[] { cuso4, naoh }, new[] { naoh }, ReactionEffectType.Precipitate, false, precipitateColor: new Color(0.2f, 0.5f, 0.9f)));

            // 4. Sodium + Water (Explosion)
            var sodiumReact = CreateConceptReaction("Sodium + Water", "2Na + 2H2O → 2NaOH + H2↑",
                new[] { na, h2o }, new[] { naoh }, ReactionEffectType.GasEvolution, true, enthalpy: -180f);
            sodiumReact.secondaryEffects.Add(ReactionEffectType.Explosion);
            sodiumReact.secondaryEffects.Add(ReactionEffectType.Flame);
            reactions.Add(sodiumReact);

            // 5. Potassium + Water (BIG Explosion)
            var potassiumReact = CreateConceptReaction("Potassium + Water", "2K + 2H2O → 2KOH + H2↑ 💥",
                new[] { k, h2o }, new[] { h2o }, ReactionEffectType.Explosion, true, enthalpy: -250f);
            potassiumReact.secondaryEffects.Add(ReactionEffectType.Flame);
            reactions.Add(potassiumReact);

            // 6. Elephant's Toothpaste
            var elephant = CreateConceptReaction("Elephant's Toothpaste", "2H2O2 → 2H2O + O2↑ (w/ KI)",
                new[] { h2o2, ki }, new[] { h2o, ki }, ReactionEffectType.Foam, true, enthalpy: -98f);
            elephant.secondaryEffects.Add(ReactionEffectType.Heat);
            elephant.secondaryEffects.Add(ReactionEffectType.Fizzing);
            reactions.Add(elephant);

            // 7. Permanganate + Glycerin (Fire)
            var fire = CreateConceptReaction("Magic Fire", "KMnO4 + Glycerin → 🔥",
                new[] { kmno4, glycerin }, new[] { h2o }, ReactionEffectType.Flame, true, enthalpy: -400f);
            fire.secondaryEffects.Add(ReactionEffectType.Explosion); // Small pop
            reactions.Add(fire);
            
            // 8. Phenolphthalein Color Change
            reactions.Add(CreateConceptReaction("Indicator Pink", "Phenol + Base → Pink",
                new[] { phenol, naoh }, new[] { phenol }, ReactionEffectType.ColorChange, false, precipitateColor: new Color(1f, 0.3f, 0.6f)));

            // 9. Vinegar + Baking Soda (Fizzing / Volcano)
            var volcano = CreateConceptReaction("Volcano", "CH3COOH + NaHCO3 → CO2↑ + H2O",
                new[] { vinegar, bakingSoda }, new[] { h2o }, ReactionEffectType.Fizzing, false, enthalpy: 10f); // Endothermic actually
            volcano.secondaryEffects.Add(ReactionEffectType.GasEvolution);
            reactions.Add(volcano);

            // 10. Ammonia + HCl (Smoke)
            reactions.Add(CreateConceptReaction("White Smoke", "NH3 + HCl → NH4Cl(s) ☁",
                new[] { ammonia, hcl }, new[] { nacl }, ReactionEffectType.Smoke, true, enthalpy: -176f, precipitateColor: Color.white));

            // 11. Magnesium Burning
            // Requires heat theoretically, but simplified for "Mix w/ Fire" or direct toggle if we support that.
            // Assuming we just "heat" it or if it touches fire.
            var mgBurn = CreateConceptReaction("Magnesium Burn", "2Mg + O2 → 2MgO + Light ✨",
                new[] { mg }, new[] { mg }, ReactionEffectType.Flame, true, enthalpy: -600f); 
            mgBurn.requiresHeat = true;
            mgBurn.minTemperature = 100f; // Requires burner
            mgBurn.secondaryEffects.Add(ReactionEffectType.Heat);
            reactions.Add(mgBurn);

            // 12. Dissolving Salt
            // NaCl(s) + H2O -> NaCl(aq)
            // Just for visual completeness
            // reactions.Add(CreateConceptReaction("Dissolve Salt", "NaCl(s) + H2O → NaCl(aq)",
            //    new[] { nacl, h2o }, new[] { nacl }, ReactionEffectType.Dissolve, false)); // Need distinct Solid vs Aqueous data if careful, but for now skipped to avoid dupes logic.


        }

        private ChemicalData CreateConceptChemical(string formula, string name, ChemicalCategory cat, Color col, float ph)
        {
            ChemicalData c = ScriptableObject.CreateInstance<ChemicalData>();
            c.name = name;
            c.chemicalName = name;
            c.formula = formula;
            c.category = cat;
            c.color = col;
            c.pH = ph;
            c.state = PhysicalState.Aqueous; // Default
            c.isTransparent = col.a < 0.9f;
            return c;
        }

        private Reaction CreateConceptReaction(string name, string eq, ChemicalData[] r, ChemicalData[] p, 
            ReactionEffectType effect, bool exo, float enthalpy = 0f, Color precipitateColor = default)
        {
            Reaction rx = ScriptableObject.CreateInstance<Reaction>();
            rx.name = name;
            rx.reactionName = name;
            rx.equationText = eq;
            rx.primaryEffect = effect;
            rx.isExothermic = exo;
            rx.enthalpyChange = enthalpy;
            rx.precipitateColor = precipitateColor;
            
            foreach (var ch in r) rx.reactants.Add(new ReactionComponent { chemical = ch });
            foreach (var ch in p) rx.products.Add(new ReactionComponent { chemical = ch });
            
            return rx;
        }

        /// <summary>
        /// Get a chemical by its formula (e.g., "H2O", "NaCl")
        /// </summary>
        public ChemicalData GetByFormula(string formula)
        {
            if (_chemicalsByFormula == null) Initialize();
            
            _chemicalsByFormula.TryGetValue(formula.ToLower(), out ChemicalData result);
            return result;
        }

        /// <summary>
        /// Get a chemical by its name
        /// </summary>
        public ChemicalData GetByName(string name)
        {
            if (_chemicalsByName == null) Initialize();
            
            _chemicalsByName.TryGetValue(name.ToLower(), out ChemicalData result);
            return result;
        }

        /// <summary>
        /// Get all chemicals in a specific category
        /// </summary>
        public List<ChemicalData> GetByCategory(ChemicalCategory category)
        {
            return chemicals.Where(c => c != null && c.category == category).ToList();
        }

        /// <summary>
        /// Find a reaction that can occur between the given chemicals
        /// </summary>
        public Reaction FindReaction(List<ChemicalData> availableChemicals, float temperature = 25f)
        {
            foreach (var reaction in reactions)
            {
                if (reaction == null) continue;
                
                // Check if all reactants are present
                if (!reaction.HasAllReactants(availableChemicals))
                    continue;
                
                // Check temperature requirements
                if (reaction.requiresHeat && temperature < reaction.minTemperature)
                    continue;
                
                if (temperature > reaction.maxTemperature)
                    continue;
                
                return reaction;
            }
            return null;
        }

        /// <summary>
        /// Find all possible reactions between two specific chemicals
        /// </summary>
        public List<Reaction> FindReactionsBetween(ChemicalData chem1, ChemicalData chem2)
        {
            var result = new List<Reaction>();
            foreach (var reaction in reactions)
            {
                if (reaction != null && reaction.CanReact(chem1, chem2))
                    result.Add(reaction);
            }
            return result;
        }

        /// <summary>
        /// Get all acids in the database
        /// </summary>
        public List<ChemicalData> GetAcids() => GetByCategory(ChemicalCategory.Acid);

        /// <summary>
        /// Get all bases in the database
        /// </summary>
        public List<ChemicalData> GetBases() => GetByCategory(ChemicalCategory.Base);

        /// <summary>
        /// Get all salts in the database
        /// </summary>
        public List<ChemicalData> GetSalts() => GetByCategory(ChemicalCategory.Salt);

        /// <summary>
        /// Get all indicators in the database
        /// </summary>
        public List<ChemicalData> GetIndicators() => GetByCategory(ChemicalCategory.Indicator);

#if UNITY_EDITOR
        /// <summary>
        /// Editor utility to refresh the database
        /// </summary>
        [ContextMenu("Refresh Database")]
        public void RefreshDatabase()
        {
            Initialize();
            Debug.Log($"ChemicalDatabase refreshed: {chemicals.Count} chemicals, {reactions.Count} reactions");
        }
#endif
    }
}
