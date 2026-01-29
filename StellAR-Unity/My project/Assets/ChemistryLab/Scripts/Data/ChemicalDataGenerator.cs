using UnityEngine;
#if UNITY_EDITOR
using UnityEditor;
#endif

namespace ChemistryLab.Data
{
    /// <summary>
    /// Editor utility to create default chemical data assets.
    /// Run this from Unity Editor menu: Chemistry Lab > Create Default Chemicals
    /// </summary>
    public static class ChemicalDataGenerator
    {
#if UNITY_EDITOR
        private const string CHEMICAL_PATH = "Assets/ChemistryLab/Data/Chemicals/";
        private const string DATABASE_PATH = "Assets/ChemistryLab/Data/";
        private const string REACTIONS_PATH = "Assets/ChemistryLab/Data/Reactions/";

        [MenuItem("Chemistry Lab/Create Default Chemicals")]
        public static void CreateDefaultChemicals()
        {
            // Ensure directories exist
            System.IO.Directory.CreateDirectory(Application.dataPath + "/ChemistryLab/Data/Chemicals");
            System.IO.Directory.CreateDirectory(Application.dataPath + "/ChemistryLab/Data/Reactions");

            // Create chemicals with more visible colors
            var hcl = CreateChemical("HCl", "Hydrochloric Acid", ChemicalCategory.Acid, 
                new Color(0.95f, 0.9f, 0.7f, 0.7f), 1f, 36.46f, "Corrosive acid"); // Slight yellow tint
            var naoh = CreateChemical("NaOH", "Sodium Hydroxide", ChemicalCategory.Base,
                new Color(0.85f, 0.9f, 1f, 0.6f), 14f, 40f, "Caustic base, causes burns"); // Slight blue tint
            var nacl = CreateChemical("NaCl", "Sodium Chloride", ChemicalCategory.Salt,
                new Color(0.95f, 0.95f, 0.9f, 0.5f), 7f, 58.44f, "Common table salt", true, Color.yellow);
            var agno3 = CreateChemical("AgNO3", "Silver Nitrate", ChemicalCategory.Salt,
                new Color(0.9f, 0.9f, 0.95f, 0.6f), 7f, 169.87f, "Light sensitive, stains skin");
            var cuso4 = CreateChemical("CuSO4", "Copper Sulfate", ChemicalCategory.Salt,
                new Color(0.1f, 0.5f, 0.95f, 0.85f), 7f, 159.6f, "Toxic if ingested", true, Color.green); // Bright blue!
            var phenol = CreateChemical("C20H14O4", "Phenolphthalein", ChemicalCategory.Indicator,
                new Color(0.95f, 0.85f, 0.9f, 0.5f), 7f, 318.32f, "pH indicator - pink in base");
            var na = CreateChemical("Na", "Sodium", ChemicalCategory.Metal,
                new Color(0.8f, 0.8f, 0.85f, 1f), 7f, 22.99f, "Reacts violently with water!", true, Color.yellow);
            na.reactsWithWater = true;
            na.isFlammable = true;
            na.state = PhysicalState.Solid;
            EditorUtility.SetDirty(na);
            
            var h2o = CreateChemical("H2O", "Water", ChemicalCategory.Solvent,
                new Color(0.4f, 0.7f, 1f, 0.65f), 7f, 18.015f, "Universal solvent"); // More visible blue
            
            // Create database
            var database = ScriptableObject.CreateInstance<ChemicalDatabase>();
            database.chemicals.Add(hcl);
            database.chemicals.Add(naoh);
            database.chemicals.Add(nacl);
            database.chemicals.Add(agno3);
            database.chemicals.Add(cuso4);
            database.chemicals.Add(phenol);
            database.chemicals.Add(na);
            database.chemicals.Add(h2o);

            // Create reactions
            var neutralization = CreateReaction("Neutralization", "HCl + NaOH → NaCl + H2O",
                "Acid-base neutralization producing salt and water",
                new[] { hcl, naoh }, new[] { nacl, h2o },
                ReactionEffectType.Heat, true);
            
            var precipitation = CreateReaction("Silver Chloride Precipitation", "AgNO3 + NaCl → AgCl↓ + NaNO3",
                "Forms white silver chloride precipitate",
                new[] { agno3, nacl }, new ChemicalData[] { nacl }, // Simplified products
                ReactionEffectType.Precipitate, false, Color.white);
            
            var copperHydroxide = CreateReaction("Copper Hydroxide Formation", "CuSO4 + 2NaOH → Cu(OH)2↓ + Na2SO4",
                "Forms blue copper hydroxide precipitate",
                new[] { cuso4, naoh }, new ChemicalData[] { naoh },
                ReactionEffectType.Precipitate, false, new Color(0.2f, 0.5f, 0.9f));
            
            var sodiumWater = CreateReaction("Sodium + Water", "2Na + 2H2O → 2NaOH + H2↑",
                "Violent reaction producing hydrogen gas and heat",
                new[] { na, h2o }, new[] { naoh },
                ReactionEffectType.GasEvolution, true);
            sodiumWater.secondaryEffects.Add(ReactionEffectType.Flame);
            sodiumWater.secondaryEffects.Add(ReactionEffectType.Explosion);
            EditorUtility.SetDirty(sodiumWater);

            var indicatorBase = CreateReaction("Phenolphthalein + Base", "Indicator turns pink in basic solution",
                "Phenolphthalein color change in alkaline environment",
                new[] { phenol, naoh }, new[] { phenol },
                ReactionEffectType.ColorChange, false, new Color(1f, 0.3f, 0.5f));

            // === NEW EXPLOSIVE CHEMICALS ===
            var k = CreateChemical("K", "Potassium", ChemicalCategory.Metal,
                new Color(0.75f, 0.75f, 0.8f, 1f), 7f, 39.1f, "EXTREMELY reactive with water - EXPLOSIVE!", true, new Color(0.8f, 0.5f, 1f));
            k.reactsWithWater = true;
            k.isFlammable = true;
            k.state = PhysicalState.Solid;
            EditorUtility.SetDirty(k);
            
            var h2o2 = CreateChemical("H2O2", "Hydrogen Peroxide", ChemicalCategory.Oxidizer,
                new Color(0.95f, 0.95f, 1f, 0.4f), 6f, 34.01f, "Strong oxidizer - can cause fires", false, Color.clear);
            
            var kMnO4 = CreateChemical("KMnO4", "Potassium Permanganate", ChemicalCategory.Oxidizer,
                new Color(0.5f, 0f, 0.5f, 0.9f), 7f, 158.03f, "Strong oxidizer - purple color", false, Color.clear);
            
            var glycerin = CreateChemical("C3H8O3", "Glycerin", ChemicalCategory.Organic,
                new Color(1f, 0.95f, 0.9f, 0.3f), 7f, 92.09f, "Flammable organic compound", false, Color.clear);
            glycerin.isFlammable = true;
            EditorUtility.SetDirty(glycerin);

            database.chemicals.Add(k);
            database.chemicals.Add(h2o2);
            database.chemicals.Add(kMnO4);
            database.chemicals.Add(glycerin);

            // === NEW EXPLOSIVE REACTIONS ===
            
            // Potassium + Water = VERY explosive
            var potassiumWater = CreateReaction("Potassium + Water EXPLOSION", "2K + 2H2O → 2KOH + H2↑ 💥",
                "EXTREMELY violent reaction - potassium explodes on contact with water!",
                new[] { k, h2o }, new[] { h2o },
                ReactionEffectType.Explosion, true);
            potassiumWater.secondaryEffects.Add(ReactionEffectType.Flame);
            potassiumWater.enthalpyChange = -196f; // Very exothermic
            EditorUtility.SetDirty(potassiumWater);

            // Permanganate + Glycerin = Spontaneous fire
            var permanganateGlycerin = CreateReaction("Permanganate + Glycerin Fire", "KMnO4 + C3H8O3 → 🔥",
                "Spontaneous combustion! The mixture catches fire after a few seconds.",
                new[] { kMnO4, glycerin }, new[] { h2o },
                ReactionEffectType.Flame, true);
            permanganateGlycerin.secondaryEffects.Add(ReactionEffectType.Explosion);
            permanganateGlycerin.enthalpyChange = -300f;
            EditorUtility.SetDirty(permanganateGlycerin);

            // Hydrogen Peroxide decomposition (with heat)
            var peroxideDecomp = CreateReaction("Peroxide Decomposition", "2H2O2 → 2H2O + O2↑",
                "Hydrogen peroxide rapidly decomposes when heated, releasing oxygen",
                new[] { h2o2, h2o2 }, new[] { h2o },
                ReactionEffectType.GasEvolution, true);
            peroxideDecomp.requiresHeat = true;
            peroxideDecomp.minTemperature = 50f;
            EditorUtility.SetDirty(peroxideDecomp);

            // Water boiling (at 100°C)
            var waterBoiling = CreateReaction("Water Boiling", "H2O(l) → H2O(g)↑",
                "Water boils and turns to steam at 100°C",
                new[] { h2o }, new[] { h2o },
                ReactionEffectType.GasEvolution, true);
            waterBoiling.requiresHeat = true;
            waterBoiling.minTemperature = 100f;
            waterBoiling.secondaryEffects.Add(ReactionEffectType.Heat);
            EditorUtility.SetDirty(waterBoiling);

            // Acid + Heat = Fuming (concentrated acids fume when heated)
            var acidFuming = CreateReaction("Acid Fuming", "HCl(aq) + heat → HCl(g)↑",
                "Heating concentrated acid releases corrosive fumes",
                new[] { hcl }, new[] { hcl },
                ReactionEffectType.GasEvolution, false);
            acidFuming.requiresHeat = true;
            acidFuming.minTemperature = 80f;
            EditorUtility.SetDirty(acidFuming);

            // New Chemicals for Elephant's Toothpaste
            var ki = CreateChemical("KI", "Potassium Iodide", ChemicalCategory.Salt, Color.white, 7f, 166.0f, "Catalyst");
            ki.state = PhysicalState.Solid;
            database.chemicals.Add(ki);

            var soap = CreateChemical("Soap", "Dish Soap", ChemicalCategory.Other, new Color(0.2f, 0.8f, 0.4f), 8f, 300f, "Foaming agent");
            soap.state = PhysicalState.Liquid; // Viscous liquid
            database.chemicals.Add(soap);

            // Elephant's Toothpaste Reaction
            // 2H2O2 -> 2H2O + O2 (catalyzed by KI)
            var elephantToothpaste = CreateReaction("Elephant's Toothpaste", "2H2O2 + 2KI → 2H2O + 2KI + O2↑",
                "Rapid decomposition releasing massive amounts of oxygen foam!",
                new[] { h2o2, ki }, new[] { h2o, ki },
                ReactionEffectType.Foam, true);
            elephantToothpaste.enthalpyChange = -98.0f; // Exothermic
            elephantToothpaste.secondaryEffects.Add(ReactionEffectType.Heat);
            elephantToothpaste.secondaryEffects.Add(ReactionEffectType.GasEvolution);
            elephantToothpaste.secondaryEffects.Add(ReactionEffectType.Fizzing);
            EditorUtility.SetDirty(elephantToothpaste);

            database.reactions.Add(neutralization);
            database.reactions.Add(precipitation);
            database.reactions.Add(copperHydroxide);
            database.reactions.Add(sodiumWater);
            database.reactions.Add(indicatorBase);
            database.reactions.Add(potassiumWater);
            database.reactions.Add(permanganateGlycerin);
            database.reactions.Add(peroxideDecomp);
            database.reactions.Add(waterBoiling);
            database.reactions.Add(acidFuming);
            database.reactions.Add(elephantToothpaste);

            AssetDatabase.CreateAsset(database, DATABASE_PATH + "ChemicalDatabase.asset");
            AssetDatabase.SaveAssets();
            AssetDatabase.Refresh();

            Debug.Log("Created default chemicals (including explosives!), reactions, and database!");
        }

        private static ChemicalData CreateChemical(string formula, string name, ChemicalCategory category,
            Color color, float pH, float molecularWeight, string hazard, 
            bool hasFlameColor = false, Color flameColor = default)
        {
            var chemical = ScriptableObject.CreateInstance<ChemicalData>();
            chemical.formula = formula;
            chemical.chemicalName = name;
            chemical.category = category;
            chemical.color = color;
            chemical.pH = pH;
            chemical.molecularWeight = molecularWeight;
            chemical.hazardInfo = hazard;
            chemical.hasFlameColor = hasFlameColor;
            chemical.flameColor = hasFlameColor ? flameColor : Color.yellow;
            chemical.state = PhysicalState.Aqueous;
            chemical.isTransparent = color.a < 0.9f;
            // Use the chemical name for the filename (safer than formula which may have special chars)
            string safeName = name.Replace(" ", "_").Replace("/", "_");
            string path = CHEMICAL_PATH + safeName + ".asset";
            AssetDatabase.CreateAsset(chemical, path);
            return chemical;
        }

        private static Reaction CreateReaction(string name, string equation, string description,
            ChemicalData[] reactants, ChemicalData[] products,
            ReactionEffectType primaryEffect, bool exothermic, Color precipitateColor = default)
        {
            var reaction = ScriptableObject.CreateInstance<Reaction>();
            reaction.reactionName = name;
            reaction.equationText = equation;
            reaction.description = description;
            reaction.primaryEffect = primaryEffect;
            reaction.isExothermic = exothermic;
            reaction.precipitateColor = precipitateColor;
            reaction.minTemperature = 20f;
            reaction.maxTemperature = 200f;

            foreach (var r in reactants)
            {
                reaction.reactants.Add(new ReactionComponent { chemical = r, coefficient = 1, moles = 1 });
            }
            foreach (var p in products)
            {
                reaction.products.Add(new ReactionComponent { chemical = p, coefficient = 1, moles = 1 });
            }

            string safeName = name.Replace(" ", "_").Replace("+", "Plus");
            string path = REACTIONS_PATH + safeName + ".asset";
            AssetDatabase.CreateAsset(reaction, path);
            return reaction;
        }
#endif
    }
}
