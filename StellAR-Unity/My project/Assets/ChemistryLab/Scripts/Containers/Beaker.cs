using UnityEngine;

namespace ChemistryLab.Containers
{
    /// <summary>
    /// Beaker container - larger capacity, for mixing chemicals
    /// </summary>
    public class Beaker : ChemicalContainer
    {
        [Header("Beaker Specific")]
        [SerializeField] private Transform stirPoint;
        [SerializeField] private ParticleSystem bubbleParticles;
        [SerializeField] private ParticleSystem steamParticles;

        [Header("Heating")]
        [SerializeField] private float boilingPoint = 100f;
        private bool _isBoiling = false;

        protected override void Start()
        {
            base.Start();
            containerName = "Beaker";
            maxCapacity = 250f; // 250mL beaker
            canPour = true;
            pourAngleThreshold = 45f;
        }

        protected override void Update()
        {
            base.Update();

            // Check for boiling
            bool shouldBoil = temperature >= boilingPoint && !IsEmpty;
            if (shouldBoil != _isBoiling)
            {
                _isBoiling = shouldBoil;
                UpdateBoilingEffects();
            }

            // Evaporation when boiling
            if (_isBoiling && currentVolume > 0)
            {
                float evaporationRate = (temperature - boilingPoint) * 0.01f;
                currentVolume = Mathf.Max(0, currentVolume - evaporationRate * Time.deltaTime);
                UpdateVisuals();
            }
        }

        private void UpdateBoilingEffects()
        {
            if (bubbleParticles != null)
            {
                if (_isBoiling)
                    bubbleParticles.Play();
                else
                    bubbleParticles.Stop();
            }

            if (steamParticles != null)
            {
                if (_isBoiling)
                    steamParticles.Play();
                else
                    steamParticles.Stop();
            }
        }

        /// <summary>
        /// Stir the contents (could speed up reactions)
        /// </summary>
        public void Stir()
        {
            // Trigger reaction check
            CheckForReactions();
        }

        /// <summary>
        /// Check if contents are boiling
        /// </summary>
        public bool IsBoiling => _isBoiling;
    }
}
