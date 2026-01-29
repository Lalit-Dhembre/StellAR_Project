using UnityEngine;
using System.Collections;

namespace ChemistryLab.Effects
{
    /// <summary>
    /// Animates liquid in containers - fill level, color transitions, sloshing
    /// </summary>
    public class LiquidAnimator : MonoBehaviour
    {
        [Header("Liquid Settings")]
        [SerializeField] private Transform liquidTransform;
        [SerializeField] private Renderer liquidRenderer;
        [SerializeField] private float fillSpeed = 2f;
        [SerializeField] private float colorTransitionSpeed = 1.5f;

        [Header("Sloshing")]
        [SerializeField] private bool enableSloshing = true;
        [SerializeField] private float sloshAmount = 0.02f;
        [SerializeField] private float sloshSpeed = 3f;

        private float _targetFillLevel = 0f;
        private float _currentFillLevel = 0f;
        private Color _targetColor = Color.clear;
        private Color _currentColor = Color.clear;
        private Vector3 _lastPosition;
        private float _sloshOffset = 0f;
        private Containers.ChemicalContainer _container;

        private void Start()
        {
            _container = GetComponent<Containers.ChemicalContainer>();
            _lastPosition = transform.position;

            // Auto-find liquid components if not assigned
            if (liquidTransform == null)
            {
                var liquid = transform.Find("Liquid");
                if (liquid != null)
                {
                    liquidTransform = liquid;
                    liquidRenderer = liquid.GetComponent<Renderer>();
                }
            }

            // Subscribe to container events
            if (_container != null)
            {
                _container.OnContentsChanged += OnContentsChanged;
            }
        }

        private void Update()
        {
            // Animate fill level
            if (!Mathf.Approximately(_currentFillLevel, _targetFillLevel))
            {
                _currentFillLevel = Mathf.MoveTowards(_currentFillLevel, _targetFillLevel, 
                    fillSpeed * Time.deltaTime);
                UpdateLiquidVisual();
            }

            // Animate color
            if (_currentColor != _targetColor)
            {
                _currentColor = Color.Lerp(_currentColor, _targetColor, 
                    colorTransitionSpeed * Time.deltaTime);
                UpdateLiquidColor();
            }

            // Sloshing effect based on movement
            if (enableSloshing && liquidTransform != null)
            {
                Vector3 movement = transform.position - _lastPosition;
                _sloshOffset += movement.magnitude * 50f;
                _sloshOffset *= 0.95f; // Damping

                float slosh = Mathf.Sin(Time.time * sloshSpeed) * _sloshOffset * sloshAmount;
                liquidTransform.localRotation = Quaternion.Euler(slosh * 10f, 0, slosh * 10f);
                
                _lastPosition = transform.position;
            }
        }

        private void OnContentsChanged(Containers.ChemicalContainer container)
        {
            // Update target fill level based on container volume
            _targetFillLevel = container.FillPercent;
            _targetColor = container.CurrentColor;

            // Play pour effect
            if (RealisticEffects.Instance != null)
            {
                RealisticEffects.Instance.PlayPourSound();
            }
        }

        private void UpdateLiquidVisual()
        {
            if (liquidTransform == null) return;

            // Scale liquid to fill level
            Vector3 scale = liquidTransform.localScale;
            scale.y = Mathf.Max(0.01f, _currentFillLevel);
            liquidTransform.localScale = scale;

            // Position liquid at bottom
            Vector3 pos = liquidTransform.localPosition;
            pos.y = (_currentFillLevel * 0.5f) - 0.5f;
            liquidTransform.localPosition = pos;
        }

        private void UpdateLiquidColor()
        {
            if (liquidRenderer == null) return;

            Color c = _currentColor;
            if (c.a < 0.1f)
            {
                // Default water-like color
                c = new Color(0.7f, 0.85f, 1f, 0.4f);
            }
            liquidRenderer.material.color = c;
        }

        /// <summary>
        /// Trigger a splash effect when liquid is added
        /// </summary>
        public void PlaySplash()
        {
            StartCoroutine(SplashAnimation());
        }

        private IEnumerator SplashAnimation()
        {
            if (liquidTransform == null) yield break;

            Vector3 originalScale = liquidTransform.localScale;
            float elapsed = 0f;
            float duration = 0.3f;

            while (elapsed < duration)
            {
                elapsed += Time.deltaTime;
                float progress = elapsed / duration;

                // Quick scale up then back
                float scaleMod = 1f + Mathf.Sin(progress * Mathf.PI) * 0.1f;
                liquidTransform.localScale = new Vector3(
                    originalScale.x * scaleMod,
                    originalScale.y,
                    originalScale.z * scaleMod
                );

                yield return null;
            }

            liquidTransform.localScale = originalScale;
        }

        private void OnDestroy()
        {
            if (_container != null)
            {
                _container.OnContentsChanged -= OnContentsChanged;
            }
        }
    }
}
