using UnityEngine;
using System.Collections.Generic;

namespace ChemistryLab.Core
{
    using Data;

    /// <summary>
    /// Result of a chemical reaction
    /// </summary>
    public class ReactionResult
    {
        public Reaction reaction;
        public List<ChemicalData> productsCreated = new List<ChemicalData>();
        public ReactionEffectType primaryEffect;
        public List<ReactionEffectType> allEffects = new List<ReactionEffectType>();
        public Color resultColor = Color.clear;
        public float temperatureChange = 0f;
        public float newPH = 7f;
        public bool isSuccessful = false;
        public string message = "";
    }

    /// <summary>
    /// Core engine that processes chemical reactions.
    /// Singleton for easy access.
    /// </summary>
    public class ReactionEngine : MonoBehaviour
    {
        public static ReactionEngine Instance { get; private set; }

        [Header("Database")]
        [SerializeField] private ChemicalDatabase database;

        [Header("Settings")]
        [SerializeField] private float defaultTemperature = 25f;
        [SerializeField] private float reactionCooldown = 0.5f;

        [Header("Debug")]
        [SerializeField] private bool debugMode = true; // Enabled by default for troubleshooting

        // Events
        public System.Action<ReactionResult> OnReactionOccurred;
        public System.Action<ChemicalData, ChemicalData> OnChemicalsMixed;

        private float _lastReactionTime;

        private void Awake()
        {
            if (Instance != null && Instance != this)
            {
                Destroy(gameObject);
                return;
            }
            Instance = this;

            // Auto-find database if not assigned
            if (database == null)
            {
                database = FindDatabase();
            }

            // Fallback: Create in-memory database if still null
            if (database == null)
            {
                Debug.LogWarning("[ReactionEngine] No ChemicalDatabase asset found. Creating temporary in-memory database.");
                database = ScriptableObject.CreateInstance<ChemicalDatabase>();
                // Initialize will trigger PopulateDefaults because the lists are empty
            }

            if (database != null)
            {
                database.Initialize();
                Debug.Log($"[ReactionEngine] Database loaded with {database.chemicals.Count} chemicals and {database.reactions.Count} reactions");
            }
            else
            {
                Debug.LogError("[ReactionEngine] Failed to create ChemicalDatabase! Reactions will not work.");
            }
        }

        /// <summary>
        /// Find the ChemicalDatabase asset
        /// </summary>
        private ChemicalDatabase FindDatabase()
        {
            // Try to find in Resources
            var db = Resources.Load<ChemicalDatabase>("ChemicalDatabase");
            if (db != null) return db;

            // Try to find any loaded database
            var allDatabases = Resources.FindObjectsOfTypeAll<ChemicalDatabase>();
            if (allDatabases.Length > 0)
            {
                Debug.Log($"[ReactionEngine] Found database: {allDatabases[0].name}");
                return allDatabases[0];
            }

            return null;
        }

        /// <summary>
        /// Set the database at runtime
        /// </summary>
        public void SetDatabase(ChemicalDatabase db)
        {
            database = db;
            if (database != null)
            {
                database.Initialize();
                Debug.Log($"[ReactionEngine] Database set: {database.chemicals.Count} chemicals, {database.reactions.Count} reactions");
            }
        }

        /// <summary>
        /// Get the chemical database
        /// </summary>
        public ChemicalDatabase Database => database;

        /// <summary>
        /// Check if the engine has a valid database
        /// </summary>
        public bool HasDatabase => database != null && database.chemicals.Count > 0;

        /// <summary>
        /// Process a potential reaction between chemicals in a container
        /// </summary>
        public ReactionResult ProcessReaction(List<ChemicalData> chemicals, float temperature = -1f)
        {
            if (temperature < 0) temperature = defaultTemperature;

            var result = new ReactionResult();

            // Check cooldown
            if (Time.time - _lastReactionTime < reactionCooldown)
            {
                result.message = "Reaction cooling down...";
                return result;
            }

            if (chemicals == null || chemicals.Count < 1)
            {
                result.message = "Need at least 1 chemical to react";
                return result;
            }

            // check for single chemical reaction (e.g. heating/decomposition)
            if (chemicals.Count == 1 && temperature < 50f)
            {
                 // Optimization: Don't check database for cold single chemicals unless they are volatile?
                 // For now, let it pass to FindReaction.
            }

            // Find applicable reaction
            Reaction reaction = database?.FindReaction(chemicals, temperature);
            
            if (reaction == null)
            {
                result.message = "No reaction occurred";
                result.resultColor = BlendColors(chemicals);
                return result;
            }

            // Process the reaction
            result.reaction = reaction;
            result.isSuccessful = true;
            result.primaryEffect = reaction.primaryEffect;
            result.allEffects.Add(reaction.primaryEffect);
            result.allEffects.AddRange(reaction.secondaryEffects);

            // Calculate products
            foreach (var product in reaction.products)
            {
                if (product.chemical != null)
                    result.productsCreated.Add(product.chemical);
            }

            // Calculate result color
            if (reaction.primaryEffect == ReactionEffectType.Precipitate)
            {
                result.resultColor = reaction.precipitateColor;
            }
            else if (result.productsCreated.Count > 0)
            {
                result.resultColor = BlendColors(result.productsCreated);
            }

            // Temperature change for exothermic/endothermic reactions
            if (reaction.isExothermic)
            {
                result.temperatureChange = Mathf.Abs(reaction.enthalpyChange) * 0.1f;
            }
            else
            {
                result.temperatureChange = -Mathf.Abs(reaction.enthalpyChange) * 0.1f;
            }

            // Calculate new pH
            result.newPH = CalculateResultPH(result.productsCreated);

            result.message = $"Reaction: {reaction.GetEquation()}";
            _lastReactionTime = Time.time;

            if (debugMode)
            {
                Debug.Log($"[ReactionEngine] {result.message}");
            }

            // Fire event
            OnReactionOccurred?.Invoke(result);

            return result;
        }

        /// <summary>
        /// Quick check if two specific chemicals can react
        /// </summary>
        public bool CanReact(ChemicalData chem1, ChemicalData chem2, float temperature = -1f)
        {
            if (temperature < 0) temperature = defaultTemperature;
            
            var reactions = database?.FindReactionsBetween(chem1, chem2);
            if (reactions == null || reactions.Count == 0) return false;

            foreach (var r in reactions)
            {
                if (!r.requiresHeat || temperature >= r.minTemperature)
                    return true;
            }
            return false;
        }

        /// <summary>
        /// Blend colors of multiple chemicals
        /// </summary>
        public Color BlendColors(List<ChemicalData> chemicals)
        {
            if (chemicals == null || chemicals.Count == 0)
                return Color.clear;

            Color result = Color.clear;
            int colorCount = 0;

            foreach (var chem in chemicals)
            {
                if (chem != null && chem.color.a > 0.01f)
                {
                    result += chem.color;
                    colorCount++;
                }
            }

            if (colorCount > 0)
            {
                result /= colorCount;
                result.a = Mathf.Min(result.a, 0.8f); // Keep some transparency
            }

            return result;
        }

        /// <summary>
        /// Calculate resulting pH when chemicals mix
        /// </summary>
        public float CalculateResultPH(List<ChemicalData> chemicals)
        {
            if (chemicals == null || chemicals.Count == 0)
                return 7f;

            float totalPH = 0f;
            int count = 0;

            foreach (var chem in chemicals)
            {
                if (chem != null)
                {
                    totalPH += chem.pH;
                    count++;
                }
            }

            return count > 0 ? totalPH / count : 7f;
        }

        /// <summary>
        /// Get the flame color for a chemical (for flame tests)
        /// </summary>
        public Color GetFlameColor(ChemicalData chemical)
        {
            if (chemical != null && chemical.hasFlameColor)
                return chemical.flameColor;
            return new Color(1f, 0.6f, 0.2f); // Default orange flame
        }

        /// <summary>
        /// Check what color an indicator would show for the given pH
        /// </summary>
        public Color GetIndicatorColor(ChemicalData indicator, float pH)
        {
            if (indicator == null || indicator.category != ChemicalCategory.Indicator)
                return Color.clear;

            // Phenolphthalein: colorless in acid, pink in base
            if (indicator.chemicalName.ToLower().Contains("phenolphthalein"))
            {
                return pH >= 8.2f ? new Color(1f, 0.4f, 0.6f) : Color.clear;
            }

            // Generic indicator behavior
            if (pH < 7f)
                return new Color(1f, 0.3f, 0.3f); // Reddish for acid
            else if (pH > 7f)
                return new Color(0.3f, 0.3f, 1f); // Blueish for base
            else
                return new Color(0.3f, 1f, 0.3f); // Greenish for neutral
        }
    }
}
