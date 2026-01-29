using UnityEngine;
using System.Collections.Generic;

namespace ChemistryLab.Containers
{
    using Data;
    using Core;

    /// <summary>
    /// Contents of a chemical container
    /// </summary>
    [System.Serializable]
    public class ChemicalAmount
    {
        public ChemicalData chemical;
        [Range(0f, 100f)]
        public float amount = 0f; // in mL

        public ChemicalAmount(ChemicalData chem, float amt)
        {
            chemical = chem;
            amount = amt;
        }
    }

    /// <summary>
    /// Base class for all containers that can hold chemicals.
    /// Handles chemical storage, mixing, and visual representation.
    /// </summary>
    public class ChemicalContainer : MonoBehaviour
    {
        [Header("Container Properties")]
        [SerializeField] protected string containerName = "Container";
        [SerializeField] protected float maxCapacity = 100f; // in mL
        [SerializeField] protected float currentVolume = 0f;

        [Header("Contents")]
        [SerializeField] protected List<ChemicalAmount> contents = new List<ChemicalAmount>();

        [Header("Temperature")]
        [SerializeField] protected float temperature = 25f; // Celsius
        [SerializeField] protected float ambientTemperature = 25f;
        [SerializeField] protected float coolingRate = 0.5f;

        [Header("Visual")]
        [SerializeField] protected Renderer liquidRenderer;
        [SerializeField] protected Transform liquidTransform;
        [SerializeField] protected float minLiquidScale = 0.0f;
        [SerializeField] protected float maxLiquidScale = 1.0f;

        [Header("Interaction")]
        [SerializeField] protected bool canPour = true;
        [SerializeField] protected float pourAngleThreshold = 45f;
        [SerializeField] protected Transform pourPoint;

        // Current mixed color
        protected Color currentColor = Color.clear;
        protected float currentPH = 7f;

        // Events
        public System.Action<ChemicalContainer> OnContentsChanged;
        public System.Action<ReactionResult> OnReactionOccurred;

        // Properties
        public string ContainerName => containerName;
        public float MaxCapacity => maxCapacity;
        public float CurrentVolume => currentVolume;
        public float Temperature => temperature;
        public float CurrentPH => currentPH;
        public Color CurrentColor => currentColor;
        public bool IsEmpty => currentVolume <= 0.01f;
        public bool IsFull => currentVolume >= maxCapacity - 0.01f;
        public float FillPercent => maxCapacity > 0 ? currentVolume / maxCapacity : 0f;

        protected virtual void Start()
        {
            UpdateVisuals();
        }

        // Heating state
        private bool _isBeingHeated = false;

        protected virtual void Update()
        {
            // Gradually cool towards ambient temperature if not being actively heated
            if (!_isBeingHeated && !Mathf.Approximately(temperature, ambientTemperature))
            {
                temperature = Mathf.MoveTowards(temperature, ambientTemperature, coolingRate * Time.deltaTime);
            }
            
            // Reset heating state for next frame
            _isBeingHeated = false;
        }

        /// <summary>
        /// Apply heat to the container (called from UI or external source)
        /// </summary>
        /// <param name="heatingRate">Degrees per second</param>
        public void ApplyHeat(float heatingRate)
        {
            temperature += heatingRate * Time.deltaTime;
            _isBeingHeated = true;
            
            // Cap visual feedback or max temp if needed (e.g. 500C)
            if (temperature > 500f) temperature = 500f;
        }

        /// <summary>
        /// Add a chemical to this container
        /// </summary>
        public virtual bool AddChemical(ChemicalData chemical, float amount)
        {
            if (chemical == null || amount <= 0) return false;

            // Check capacity
            float spaceLeft = maxCapacity - currentVolume;
            float actualAmount = Mathf.Min(amount, spaceLeft);
            
            if (actualAmount <= 0) return false;

            // Check if we already have this chemical
            var existing = contents.Find(c => c.chemical == chemical);
            if (existing != null)
            {
                existing.amount += actualAmount;
            }
            else
            {
                contents.Add(new ChemicalAmount(chemical, actualAmount));
            }

            currentVolume += actualAmount;

            // Check for reactions
            CheckForReactions();

            // Update visuals
            UpdateVisuals();
            
            OnContentsChanged?.Invoke(this);
            
            return true;
        }

        /// <summary>
        /// Remove a specific amount of chemical
        /// </summary>
        public virtual float RemoveChemical(ChemicalData chemical, float amount)
        {
            var existing = contents.Find(c => c.chemical == chemical);
            if (existing == null) return 0f;

            float removed = Mathf.Min(existing.amount, amount);
            existing.amount -= removed;
            currentVolume -= removed;

            if (existing.amount <= 0.01f)
            {
                contents.Remove(existing);
            }

            UpdateVisuals();
            OnContentsChanged?.Invoke(this);

            return removed;
        }

        /// <summary>
        /// Pour contents into another container
        /// </summary>
        public virtual float PourInto(ChemicalContainer target, float amount)
        {
            if (target == null || IsEmpty) return 0f;

            float totalPoured = 0f;
            float amountPerChemical = amount / contents.Count;

            // Create a copy to iterate safely
            var contentsCopy = new List<ChemicalAmount>(contents);
            
            foreach (var chemAmount in contentsCopy)
            {
                float toPour = Mathf.Min(amountPerChemical, chemAmount.amount);
                float spaceInTarget = target.MaxCapacity - target.CurrentVolume;
                toPour = Mathf.Min(toPour, spaceInTarget);

                if (toPour > 0)
                {
                    if (target.AddChemical(chemAmount.chemical, toPour))
                    {
                        RemoveChemical(chemAmount.chemical, toPour);
                        totalPoured += toPour;
                    }
                }
            }

            return totalPoured;
        }

        /// <summary>
        /// Clear all contents
        /// </summary>
        public virtual void Empty()
        {
            contents.Clear();
            currentVolume = 0f;
            currentColor = Color.clear;
            currentPH = 7f;
            UpdateVisuals();
            OnContentsChanged?.Invoke(this);
        }

        /// <summary>
        /// Heat the container
        /// </summary>
        public virtual void Heat(float amount)
        {
            temperature += amount;
            temperature = Mathf.Clamp(temperature, -50f, 500f);

            // Check if heating triggers any reactions
            CheckForReactions();
        }

        /// <summary>
        /// Heat the container with a max temperature limit
        /// </summary>
        public virtual void Heat(float amount, float maxTemp)
        {
            if (temperature < maxTemp)
            {
                temperature += amount;
                temperature = Mathf.Min(temperature, maxTemp);
                CheckForReactions();
            }
        }

        /// <summary>
        /// Cool the container
        /// </summary>
        public virtual void Cool(float amount)
        {
            temperature -= amount;
            temperature = Mathf.Clamp(temperature, -50f, 500f);
        }

        /// <summary>
        /// Get list of all chemicals in the container
        /// </summary>
        public List<ChemicalData> GetChemicals()
        {
            var result = new List<ChemicalData>();
            foreach (var ca in contents)
            {
                if (ca.chemical != null)
                    result.Add(ca.chemical);
            }
            return result;
        }

        /// <summary>
        /// Check if container has a specific chemical
        /// </summary>
        public bool HasChemical(ChemicalData chemical)
        {
            return contents.Exists(c => c.chemical == chemical);
        }

        /// <summary>
        /// Get amount of a specific chemical
        /// </summary>
        public float GetAmount(ChemicalData chemical)
        {
            var found = contents.Find(c => c.chemical == chemical);
            return found?.amount ?? 0f;
        }

        /// <summary>
        /// Check for and process any reactions
        /// </summary>
        protected virtual void CheckForReactions()
        {
            Debug.Log($"[{containerName}] Checking for reactions... Contents: {contents.Count} chemicals");
            
            if (ReactionEngine.Instance == null)
            {
                Debug.LogWarning($"[{containerName}] No ReactionEngine found!");
                return;
            }
            
            if (!ReactionEngine.Instance.HasDatabase)
            {
                Debug.LogWarning($"[{containerName}] ReactionEngine has no database!");
                return;
            }
            
            if (contents.Count < 2)
            {
                Debug.Log($"[{containerName}] Need at least 2 chemicals to react");
                return;
            }

            var chemicals = GetChemicals();
            Debug.Log($"[{containerName}] Processing reaction with: {string.Join(", ", chemicals.ConvertAll(c => c.formula))}");
            
            var result = ReactionEngine.Instance.ProcessReaction(chemicals, temperature);

            if (result.isSuccessful)
            {
                Debug.Log($"[{containerName}] REACTION SUCCESS! {result.message}");
                
                // Apply reaction results
                currentColor = result.resultColor;
                currentPH = result.newPH;
                temperature += result.temperatureChange;

                // Replace reactants with products (simplified)
                // In a full implementation, you'd track moles and balance equations
                
                OnReactionOccurred?.Invoke(result);
                UpdateVisuals();
            }
            else
            {
                Debug.Log($"[{containerName}] No reaction: {result.message}");
                
                // Just blend colors if no reaction
                currentColor = ReactionEngine.Instance.BlendColors(chemicals);
                currentPH = ReactionEngine.Instance.CalculateResultPH(chemicals);
                UpdateVisuals();
            }
        }

        /// <summary>
        /// Update visual representation of contents
        /// </summary>
        protected virtual void UpdateVisuals()
        {
            // Update liquid level
            if (liquidTransform != null)
            {
                float fillLevel = Mathf.Lerp(minLiquidScale, maxLiquidScale, FillPercent);
                liquidTransform.localScale = new Vector3(
                    liquidTransform.localScale.x,
                    fillLevel,
                    liquidTransform.localScale.z
                );
            }

            // Update liquid color
            if (liquidRenderer != null)
            {
                var material = liquidRenderer.material;
                if (currentColor.a > 0.01f)
                {
                    material.color = currentColor;
                }
                else
                {
                    material.color = new Color(0.9f, 0.95f, 1f, 0.3f); // Default water-like
                }
            }
        }

        /// <summary>
        /// Check if container is tilted enough to pour
        /// </summary>
        public bool ShouldPour()
        {
            if (!canPour || IsEmpty) return false;

            float angle = Vector3.Angle(transform.up, Vector3.up);
            return angle > pourAngleThreshold;
        }
    }
}
