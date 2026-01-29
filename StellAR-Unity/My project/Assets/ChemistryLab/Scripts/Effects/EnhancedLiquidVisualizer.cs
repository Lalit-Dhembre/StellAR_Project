using UnityEngine;
using System.Collections;
using System.Collections.Generic;

namespace ChemistryLab.Effects
{
    using Containers;
    using Data;

    /// <summary>
    /// Enhanced liquid visualization for realistic chemical display.
    /// Creates and manages a visible liquid mesh inside containers.
    /// </summary>
    public class EnhancedLiquidVisualizer : MonoBehaviour
    {
        [Header("Liquid Appearance")]
        [SerializeField] private float liquidTransparency = 0.8f;
        [SerializeField] private float liquidShininess = 0.6f;
        [SerializeField] private bool showSurfaceRipples = true;
        [SerializeField] private bool showBubbles = true;

        [Header("Animation")]
        [SerializeField] private float fillAnimationSpeed = 2f;
        [SerializeField] private float colorBlendSpeed = 1.5f;
        [SerializeField] private float swirlingSpeed = 1f;

        private ChemicalContainer _container;
        private GameObject _liquidMesh;
        private Renderer _liquidRenderer;
        private Material _liquidMaterial;
        
        private float _currentFillLevel = 0f;
        private float _targetFillLevel = 0f;
        private Color _currentColor = Color.clear;
        private Color _targetColor = Color.clear;
        private float _swirlingPhase = 0f;
        
        // For bubble effects
        private List<GameObject> _activeBubbles = new List<GameObject>();
        private float _bubbleSpawnTimer = 0f;

        private void Start()
        {
            _container = GetComponent<ChemicalContainer>();
            CreateLiquidMesh();
            
            if (_container != null)
            {
                _container.OnContentsChanged += OnContentsChanged;
                _container.OnReactionOccurred += OnReactionOccurred;
            }
        }

        private void CreateLiquidMesh()
        {
            // Find or create liquid visualization
            Transform existingLiquid = transform.Find("LiquidVisualization");
            if (existingLiquid != null)
            {
                _liquidMesh = existingLiquid.gameObject;
            }
            else
            {
                // Create a cylinder to represent liquid INSIDE the container
                _liquidMesh = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
                _liquidMesh.name = "LiquidVisualization";
                _liquidMesh.transform.SetParent(transform);
                // Position at center-bottom of container, scaled to fit inside
                _liquidMesh.transform.localPosition = Vector3.zero;
                _liquidMesh.transform.localRotation = Quaternion.identity;
                _liquidMesh.transform.localScale = new Vector3(0.4f, 0.01f, 0.4f); // Smaller to fit inside container
                
                // Remove collider - it's just visual
                Destroy(_liquidMesh.GetComponent<Collider>());
            }

            _liquidRenderer = _liquidMesh.GetComponent<Renderer>();
            if (_liquidRenderer != null)
            {
                // Create a transparent material for liquid
                _liquidMaterial = new Material(Shader.Find("Standard"));
                if (_liquidMaterial == null)
                {
                    _liquidMaterial = new Material(Shader.Find("Universal Render Pipeline/Lit"));
                }
                
                // Set up transparency
                SetupTransparentMaterial(_liquidMaterial);
                _liquidMaterial.color = new Color(0.7f, 0.85f, 1f, 0.3f); // Default water color
                _liquidRenderer.material = _liquidMaterial;
            }

            // Start invisible
            _liquidMesh.SetActive(false);
        }

        private void SetupTransparentMaterial(Material mat)
        {
            // Configure for transparency
            mat.SetFloat("_Mode", 3); // Transparent
            mat.SetInt("_SrcBlend", (int)UnityEngine.Rendering.BlendMode.SrcAlpha);
            mat.SetInt("_DstBlend", (int)UnityEngine.Rendering.BlendMode.OneMinusSrcAlpha);
            mat.SetInt("_ZWrite", 0);
            mat.DisableKeyword("_ALPHATEST_ON");
            mat.EnableKeyword("_ALPHABLEND_ON");
            mat.DisableKeyword("_ALPHAPREMULTIPLY_ON");
            mat.renderQueue = 3000;
            
            // Add some shine
            mat.SetFloat("_Glossiness", liquidShininess);
            mat.SetFloat("_Metallic", 0.1f);
        }

        private void Update()
        {
            AnimateLiquid();
            
            if (showBubbles && _container != null && _container.Temperature > 50f)
            {
                AnimateBubbles();
            }
        }

        private void AnimateLiquid()
        {
            // Animate fill level
            if (!Mathf.Approximately(_currentFillLevel, _targetFillLevel))
            {
                _currentFillLevel = Mathf.MoveTowards(_currentFillLevel, _targetFillLevel, 
                    fillAnimationSpeed * Time.deltaTime);
                UpdateLiquidMesh();
            }

            // Animate color
            if (_currentColor != _targetColor)
            {
                _currentColor = Color.Lerp(_currentColor, _targetColor, 
                    colorBlendSpeed * Time.deltaTime);
                UpdateLiquidColor();
            }

            // Swirling animation for mixing effect - only animate color shimmer, NOT rotation
            if (_container != null && !_container.IsEmpty)
            {
                _swirlingPhase += Time.deltaTime * swirlingSpeed;
                
                // DON'T rotate the liquid - it causes the container to look wrong
                // Instead, just update the phase for color effects
            }
        }

        private void AnimateBubbles()
        {
            _bubbleSpawnTimer += Time.deltaTime;
            
            // Spawn bubbles based on temperature
            float spawnRate = (_container.Temperature - 50f) / 50f; // More bubbles = hotter
            if (_bubbleSpawnTimer > 0.2f / Mathf.Max(spawnRate, 0.1f))
            {
                _bubbleSpawnTimer = 0f;
                SpawnBubble();
            }

            // Animate existing bubbles
            for (int i = _activeBubbles.Count - 1; i >= 0; i--)
            {
                if (_activeBubbles[i] == null)
                {
                    _activeBubbles.RemoveAt(i);
                    continue;
                }

                // Move bubble up
                _activeBubbles[i].transform.localPosition += Vector3.up * Time.deltaTime * 0.1f;
                
                // Remove if above liquid surface
                if (_activeBubbles[i].transform.localPosition.y > _currentFillLevel * 0.5f)
                {
                    Destroy(_activeBubbles[i]);
                    _activeBubbles.RemoveAt(i);
                }
            }
        }

        private void SpawnBubble()
        {
            if (_liquidMesh == null) return;

            var bubble = GameObject.CreatePrimitive(PrimitiveType.Sphere);
            bubble.name = "LiquidBubble";
            bubble.transform.SetParent(_liquidMesh.transform);
            
            float size = Random.Range(0.02f, 0.06f);
            bubble.transform.localScale = Vector3.one * size;
            bubble.transform.localPosition = new Vector3(
                Random.Range(-0.3f, 0.3f),
                -0.4f,
                Random.Range(-0.3f, 0.3f)
            );

            Destroy(bubble.GetComponent<Collider>());

            var renderer = bubble.GetComponent<Renderer>();
            var mat = new Material(_liquidMaterial);
            Color bubbleColor = _currentColor;
            bubbleColor.a = 0.4f;
            mat.color = bubbleColor;
            renderer.material = mat;

            _activeBubbles.Add(bubble);

            // Auto-destroy after a while
            Destroy(bubble, 3f);
        }

        private void OnContentsChanged(ChemicalContainer container)
        {
            _targetFillLevel = container.FillPercent;
            _targetColor = CalculateLiquidColor(container);
            
            // Show liquid mesh if there's content
            if (_liquidMesh != null)
            {
                _liquidMesh.SetActive(!container.IsEmpty);
            }

            // Play splash effect when adding liquid
            if (_targetFillLevel > _currentFillLevel + 0.05f)
            {
                PlaySplashEffect();
            }
        }

        private void OnReactionOccurred(Core.ReactionResult result)
        {
            if (result == null || !result.isSuccessful) return;

            // Enhanced swirling during reaction
            StartCoroutine(PlayReactionMixingEffect(result));
        }

        private Color CalculateLiquidColor(ChemicalContainer container)
        {
            var chemicals = container.GetChemicals();
            if (chemicals.Count == 0)
            {
                return new Color(0.7f, 0.85f, 1f, 0.3f); // Clear water
            }

            // Blend colors of all chemicals
            Color blended = Color.clear;
            float totalAmount = 0f;

            foreach (var chem in chemicals)
            {
                float amount = container.GetAmount(chem);
                Color chemColor = chem.color;
                
                // Handle transparent/white chemicals
                if (chemColor.a < 0.1f || (chemColor.r > 0.9f && chemColor.g > 0.9f && chemColor.b > 0.9f))
                {
                    // Give them a subtle tint based on category
                    chemColor = GetChemicalTint(chem);
                }

                blended += chemColor * amount;
                totalAmount += amount;
            }

            if (totalAmount > 0)
            {
                blended /= totalAmount;
            }

            // Ensure good visibility
            blended.a = Mathf.Max(blended.a, liquidTransparency);
            return blended;
        }

        private Color GetChemicalTint(ChemicalData chemical)
        {
            // Assign visible colors to transparent chemicals based on category
            switch (chemical.category)
            {
                case ChemicalCategory.Acid:
                    return new Color(1f, 0.9f, 0.8f, 0.6f); // Slight yellow tint
                case ChemicalCategory.Base:
                    return new Color(0.85f, 0.9f, 1f, 0.5f); // Slight blue tint
                case ChemicalCategory.Salt:
                    return new Color(0.95f, 0.95f, 0.95f, 0.4f); // Very light gray
                case ChemicalCategory.Solvent:
                    return new Color(0.8f, 0.9f, 1f, 0.35f); // Water blue
                case ChemicalCategory.Indicator:
                    return new Color(0.9f, 0.7f, 0.9f, 0.7f); // Light purple
                default:
                    return new Color(0.85f, 0.85f, 0.9f, 0.5f);
            }
        }

        private void UpdateLiquidMesh()
        {
            if (_liquidMesh == null) return;

            // Scale the liquid cylinder to represent fill level
            // Use smaller X/Z to fit inside container
            float fillHeight = Mathf.Max(0.02f, _currentFillLevel * 0.3f);
            _liquidMesh.transform.localScale = new Vector3(0.4f, fillHeight, 0.4f);

            // Position liquid at the bottom of container, rising with fill
            // Cylinder center is at origin, so offset by half height
            float yPos = fillHeight * 0.5f; // Half the height since cylinder is centered
            _liquidMesh.transform.localPosition = new Vector3(0, yPos, 0);
            
            // Keep rotation fixed - don't let it wobble
            _liquidMesh.transform.localRotation = Quaternion.identity;
        }

        private void UpdateLiquidColor()
        {
            if (_liquidRenderer == null || _liquidMaterial == null) return;
            
            _liquidMaterial.color = _currentColor;
            
            // Emit slight glow for certain chemicals
            if (_currentColor.r > 0.8f || _currentColor.g > 0.8f || _currentColor.b > 0.8f)
            {
                _liquidMaterial.EnableKeyword("_EMISSION");
                _liquidMaterial.SetColor("_EmissionColor", _currentColor * 0.3f);
            }
        }

        private void PlaySplashEffect()
        {
            if (_liquidMesh == null) return;
            StartCoroutine(SplashAnimation());
        }

        private IEnumerator SplashAnimation()
        {
            Vector3 originalScale = _liquidMesh.transform.localScale;
            float elapsed = 0f;
            float duration = 0.4f;

            while (elapsed < duration)
            {
                elapsed += Time.deltaTime;
                float progress = elapsed / duration;

                // Quick bulge then settle
                float bulge = 1f + Mathf.Sin(progress * Mathf.PI) * 0.15f;
                _liquidMesh.transform.localScale = new Vector3(
                    originalScale.x * bulge,
                    originalScale.y,
                    originalScale.z * bulge
                );

                yield return null;
            }

            _liquidMesh.transform.localScale = originalScale;
        }

        private IEnumerator PlayReactionMixingEffect(Core.ReactionResult result)
        {
            float duration = 2f;
            float elapsed = 0f;
            float originalSwirlingSpeed = swirlingSpeed;
            
            // Speed up swirling
            swirlingSpeed = 5f;

            // Create mixing particles
            List<GameObject> mixingParticles = new List<GameObject>();
            Color color1 = _currentColor;
            Color color2 = result.resultColor;

            for (int i = 0; i < 20; i++)
            {
                var particle = GameObject.CreatePrimitive(PrimitiveType.Sphere);
                particle.name = "MixingParticle";
                particle.transform.SetParent(_liquidMesh.transform);
                particle.transform.localScale = Vector3.one * Random.Range(0.05f, 0.1f);
                particle.transform.localPosition = new Vector3(
                    Random.Range(-0.3f, 0.3f),
                    Random.Range(-0.3f, 0.1f),
                    Random.Range(-0.3f, 0.3f)
                );
                Destroy(particle.GetComponent<Collider>());

                var renderer = particle.GetComponent<Renderer>();
                var mat = new Material(_liquidMaterial);
                mat.color = Random.value > 0.5f ? color1 : color2;
                renderer.material = mat;

                mixingParticles.Add(particle);
            }

            while (elapsed < duration)
            {
                elapsed += Time.deltaTime;
                float progress = elapsed / duration;

                // Spiral the particles
                foreach (var p in mixingParticles)
                {
                    if (p != null)
                    {
                        float angle = elapsed * 5f + p.GetInstanceID() * 0.1f;
                        float radius = 0.2f * (1f - progress);
                        p.transform.localPosition = new Vector3(
                            Mathf.Cos(angle) * radius,
                            p.transform.localPosition.y,
                            Mathf.Sin(angle) * radius
                        );

                        // Fade size
                        p.transform.localScale *= 0.99f;

                        // Blend to result color
                        var renderer = p.GetComponent<Renderer>();
                        Color c = renderer.material.color;
                        renderer.material.color = Color.Lerp(c, result.resultColor, Time.deltaTime * 2f);
                    }
                }

                yield return null;
            }

            // Cleanup
            swirlingSpeed = originalSwirlingSpeed;
            foreach (var p in mixingParticles)
            {
                if (p != null) Destroy(p);
            }
        }

        private void OnDestroy()
        {
            if (_container != null)
            {
                _container.OnContentsChanged -= OnContentsChanged;
                _container.OnReactionOccurred -= OnReactionOccurred;
            }

            // Cleanup bubbles
            foreach (var b in _activeBubbles)
            {
                if (b != null) Destroy(b);
            }
        }
    }
}
