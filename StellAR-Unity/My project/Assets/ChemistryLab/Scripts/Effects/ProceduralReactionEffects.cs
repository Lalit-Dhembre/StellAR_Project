using UnityEngine;
using System.Collections;

namespace ChemistryLab.Effects
{
    using Core;
    using Data;

    /// <summary>
    /// Creates visual effects for chemical reactions using Unity primitives
    /// No prefabs needed - creates effects procedurally at runtime
    /// </summary>
    public class ProceduralReactionEffects : MonoBehaviour
    {
        [Header("Effect Settings")]
        [SerializeField] private int particleCount = 20;
        [SerializeField] private float effectDuration = 2f;
        [SerializeField] private float particleSpeed = 0.5f;

        private static ProceduralReactionEffects _instance;
        public static ProceduralReactionEffects Instance => _instance;

        private void Awake()
        {
            _instance = this;
        }

        private void Start()
        {
            // Subscribe to reaction events
            if (ReactionEngine.Instance != null)
            {
                ReactionEngine.Instance.OnReactionOccurred += OnReaction;
            }
        }

        private void OnDestroy()
        {
            if (ReactionEngine.Instance != null)
            {
                ReactionEngine.Instance.OnReactionOccurred -= OnReaction;
            }
        }

        private void OnReaction(ReactionResult result)
        {
            if (result == null || !result.isSuccessful) return;

            // Find the container that had the reaction
            var containers = FindObjectsByType<Containers.ChemicalContainer>(FindObjectsSortMode.None);
            foreach (var container in containers)
            {
                // Trigger effects on all containers that might be involved
                Vector3 pos = container.transform.position + Vector3.up * 0.1f;
                
                switch (result.primaryEffect)
                {
                    case ReactionEffectType.Precipitate:
                        StartCoroutine(PlayPrecipitateEffect(pos, result.resultColor));
                        break;
                    case ReactionEffectType.GasEvolution:
                        StartCoroutine(PlayBubbleEffect(pos));
                        break;
                    case ReactionEffectType.Heat:
                        StartCoroutine(PlayHeatEffect(pos));
                        break;
                    case ReactionEffectType.ColorChange:
                        StartCoroutine(PlayColorFlash(pos, result.resultColor));
                        break;
                    case ReactionEffectType.Flame:
                        StartCoroutine(PlayFlameEffect(pos));
                        break;
                    case ReactionEffectType.Explosion:
                        StartCoroutine(PlayExplosionEffect(pos));
                        break;
                }

                // Check for secondary effects
                foreach (var effect in result.allEffects)
                {
                    if (effect == ReactionEffectType.Explosion)
                    {
                        StartCoroutine(PlayExplosionEffect(pos));
                    }
                    else if (effect == ReactionEffectType.Flame)
                    {
                        StartCoroutine(PlayFlameEffect(pos));
                    }
                }

                // Also play a general reaction flash
                StartCoroutine(PlayReactionFlash(pos, result.resultColor));
                break; // Only play on one container
            }
        }

        /// <summary>
        /// Play a precipitate effect (particles falling down)
        /// </summary>
        public IEnumerator PlayPrecipitateEffect(Vector3 position, Color color)
        {
            Debug.Log($"[Effects] Playing precipitate effect at {position}");
            
            GameObject[] particles = new GameObject[particleCount];
            
            for (int i = 0; i < particleCount; i++)
            {
                particles[i] = GameObject.CreatePrimitive(PrimitiveType.Sphere);
                particles[i].name = "Precipitate";
                particles[i].transform.position = position + Random.insideUnitSphere * 0.05f;
                particles[i].transform.localScale = Vector3.one * Random.Range(0.005f, 0.015f);
                
                var renderer = particles[i].GetComponent<Renderer>();
                renderer.material.color = color.a > 0.1f ? color : Color.white;
                
                Destroy(particles[i].GetComponent<Collider>());

                // Add simple physics
                var rb = particles[i].AddComponent<Rigidbody>();
                rb.mass = 0.001f;
                rb.useGravity = true;
                rb.linearDamping = 2f;
            }

            yield return new WaitForSeconds(effectDuration);

            // Cleanup
            foreach (var p in particles)
            {
                if (p != null) Destroy(p);
            }
        }

        /// <summary>
        /// Play bubble effect (particles rising up)
        /// </summary>
        public IEnumerator PlayBubbleEffect(Vector3 position)
        {
            Debug.Log($"[Effects] Playing bubble effect at {position}");
            
            float elapsed = 0f;
            
            while (elapsed < effectDuration)
            {
                // Spawn a bubble
                var bubble = GameObject.CreatePrimitive(PrimitiveType.Sphere);
                bubble.name = "Bubble";
                bubble.transform.position = position + new Vector3(
                    Random.Range(-0.03f, 0.03f), 0, Random.Range(-0.03f, 0.03f));
                bubble.transform.localScale = Vector3.one * Random.Range(0.008f, 0.02f);
                
                var renderer = bubble.GetComponent<Renderer>();
                renderer.material.color = new Color(1f, 1f, 1f, 0.5f);
                
                Destroy(bubble.GetComponent<Collider>());

                // Animate bubble rising
                StartCoroutine(AnimateBubbleRising(bubble));

                elapsed += 0.1f;
                yield return new WaitForSeconds(0.1f);
            }
        }

        private IEnumerator AnimateBubbleRising(GameObject bubble)
        {
            float lifetime = Random.Range(0.5f, 1.5f);
            float elapsed = 0f;
            Vector3 startPos = bubble.transform.position;
            float wobble = Random.Range(0f, Mathf.PI * 2);

            while (elapsed < lifetime && bubble != null)
            {
                elapsed += Time.deltaTime;
                float progress = elapsed / lifetime;
                
                bubble.transform.position = startPos + new Vector3(
                    Mathf.Sin(elapsed * 5f + wobble) * 0.01f,
                    elapsed * particleSpeed,
                    Mathf.Cos(elapsed * 5f + wobble) * 0.01f
                );

                // Fade out
                var renderer = bubble.GetComponent<Renderer>();
                if (renderer != null)
                {
                    Color c = renderer.material.color;
                    c.a = 1f - progress;
                    renderer.material.color = c;
                }

                yield return null;
            }

            if (bubble != null) Destroy(bubble);
        }

        /// <summary>
        /// Play heat effect (steam/vapor rising)
        /// </summary>
        public IEnumerator PlayHeatEffect(Vector3 position)
        {
            Debug.Log($"[Effects] Playing heat effect at {position}");
            
            float elapsed = 0f;
            
            while (elapsed < effectDuration)
            {
                // Spawn steam particle
                var steam = GameObject.CreatePrimitive(PrimitiveType.Cube);
                steam.name = "Steam";
                steam.transform.position = position + new Vector3(
                    Random.Range(-0.02f, 0.02f), 0, Random.Range(-0.02f, 0.02f));
                steam.transform.localScale = Vector3.one * Random.Range(0.01f, 0.03f);
                steam.transform.rotation = Random.rotation;
                
                var renderer = steam.GetComponent<Renderer>();
                renderer.material.color = new Color(0.9f, 0.9f, 1f, 0.3f);
                
                Destroy(steam.GetComponent<Collider>());

                StartCoroutine(AnimateSteamRising(steam));

                elapsed += 0.15f;
                yield return new WaitForSeconds(0.15f);
            }
        }

        private IEnumerator AnimateSteamRising(GameObject steam)
        {
            float lifetime = Random.Range(1f, 2f);
            float elapsed = 0f;
            Vector3 startPos = steam.transform.position;
            Vector3 startScale = steam.transform.localScale;

            while (elapsed < lifetime && steam != null)
            {
                elapsed += Time.deltaTime;
                float progress = elapsed / lifetime;
                
                steam.transform.position = startPos + Vector3.up * elapsed * 0.2f;
                steam.transform.localScale = startScale * (1f + progress);

                var renderer = steam.GetComponent<Renderer>();
                if (renderer != null)
                {
                    Color c = renderer.material.color;
                    c.a = 0.3f * (1f - progress);
                    renderer.material.color = c;
                }

                yield return null;
            }

            if (steam != null) Destroy(steam);
        }

        /// <summary>
        /// Play color flash effect
        /// </summary>
        public IEnumerator PlayColorFlash(Vector3 position, Color color)
        {
            Debug.Log($"[Effects] Playing color flash at {position}");
            
            // Create a glowing sphere
            var flash = GameObject.CreatePrimitive(PrimitiveType.Sphere);
            flash.name = "ColorFlash";
            flash.transform.position = position;
            flash.transform.localScale = Vector3.one * 0.1f;
            
            var renderer = flash.GetComponent<Renderer>();
            renderer.material.color = color;
            
            Destroy(flash.GetComponent<Collider>());

            // Add light
            var light = flash.AddComponent<Light>();
            light.type = LightType.Point;
            light.color = color;
            light.intensity = 2f;
            light.range = 0.5f;

            // Animate
            float elapsed = 0f;
            Vector3 startScale = flash.transform.localScale;

            while (elapsed < 0.5f)
            {
                elapsed += Time.deltaTime;
                float progress = elapsed / 0.5f;
                
                flash.transform.localScale = startScale * (1f + progress * 2f);
                light.intensity = 2f * (1f - progress);

                var mat = flash.GetComponent<Renderer>().material;
                Color c = mat.color;
                c.a = 1f - progress;
                mat.color = c;

                yield return null;
            }

            Destroy(flash);
        }

        /// <summary>
        /// Play flame effect
        /// </summary>
        public IEnumerator PlayFlameEffect(Vector3 position)
        {
            Debug.Log($"[Effects] Playing flame effect at {position}");
            
            float elapsed = 0f;
            
            while (elapsed < effectDuration)
            {
                // Spawn flame particle
                var flame = GameObject.CreatePrimitive(PrimitiveType.Sphere);
                flame.name = "Flame";
                flame.transform.position = position + new Vector3(
                    Random.Range(-0.02f, 0.02f), 0, Random.Range(-0.02f, 0.02f));
                flame.transform.localScale = Vector3.one * Random.Range(0.015f, 0.04f);
                
                var renderer = flame.GetComponent<Renderer>();
                renderer.material.color = Color.Lerp(Color.yellow, Color.red, Random.value);
                
                Destroy(flame.GetComponent<Collider>());

                StartCoroutine(AnimateFlameRising(flame));

                elapsed += 0.05f;
                yield return new WaitForSeconds(0.05f);
            }
        }

        private IEnumerator AnimateFlameRising(GameObject flame)
        {
            float lifetime = Random.Range(0.3f, 0.8f);
            float elapsed = 0f;
            Vector3 startPos = flame.transform.position;

            while (elapsed < lifetime && flame != null)
            {
                elapsed += Time.deltaTime;
                float progress = elapsed / lifetime;
                
                flame.transform.position = startPos + Vector3.up * elapsed * 0.5f + 
                    new Vector3(Mathf.Sin(elapsed * 10f) * 0.01f, 0, 0);
                flame.transform.localScale *= 0.98f;

                var renderer = flame.GetComponent<Renderer>();
                if (renderer != null)
                {
                    renderer.material.color = Color.Lerp(Color.yellow, Color.red, progress);
                }

                yield return null;
            }

            if (flame != null) Destroy(flame);
        }

        /// <summary>
        /// General reaction flash - bright burst indicating something happened
        /// </summary>
        public IEnumerator PlayReactionFlash(Vector3 position, Color color)
        {
            // Create expanding ring
            var ring = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            ring.name = "ReactionRing";
            ring.transform.position = position;
            ring.transform.localScale = new Vector3(0.05f, 0.002f, 0.05f);
            
            var renderer = ring.GetComponent<Renderer>();
            Color ringColor = color.a > 0.1f ? color : Color.cyan;
            ringColor.a = 0.8f;
            renderer.material.color = ringColor;
            
            Destroy(ring.GetComponent<Collider>());

            // Animate ring expanding
            float elapsed = 0f;
            float duration = 0.8f;

            while (elapsed < duration)
            {
                elapsed += Time.deltaTime;
                float progress = elapsed / duration;
                
                float scale = 0.05f + progress * 0.3f;
                ring.transform.localScale = new Vector3(scale, 0.002f, scale);

                Color c = renderer.material.color;
                c.a = 0.8f * (1f - progress);
                renderer.material.color = c;

                yield return null;
            }

            Destroy(ring);
        }

        /// <summary>
        /// EXPLOSIVE REACTION - BIG DRAMATIC EFFECT!
        /// </summary>
        public IEnumerator PlayExplosionEffect(Vector3 position)
        {
            Debug.Log($"[Effects] 💥 EXPLOSION at {position}!");
            
            // Create central fireball
            var fireball = GameObject.CreatePrimitive(PrimitiveType.Sphere);
            fireball.name = "Fireball";
            fireball.transform.position = position;
            fireball.transform.localScale = Vector3.one * 0.05f;
            
            var fireRenderer = fireball.GetComponent<Renderer>();
            fireRenderer.material.color = Color.yellow;
            Destroy(fireball.GetComponent<Collider>());

            // Add bright light
            var explosionLight = fireball.AddComponent<Light>();
            explosionLight.type = LightType.Point;
            explosionLight.color = new Color(1f, 0.6f, 0.2f);
            explosionLight.intensity = 10f;
            explosionLight.range = 2f;

            // Spawn explosion debris
            GameObject[] debris = new GameObject[30];
            for (int i = 0; i < debris.Length; i++)
            {
                debris[i] = GameObject.CreatePrimitive(Random.value > 0.5f ? PrimitiveType.Sphere : PrimitiveType.Cube);
                debris[i].name = "Debris";
                debris[i].transform.position = position;
                debris[i].transform.localScale = Vector3.one * Random.Range(0.01f, 0.04f);
                debris[i].transform.rotation = Random.rotation;
                
                var debrisRenderer = debris[i].GetComponent<Renderer>();
                debrisRenderer.material.color = Color.Lerp(Color.yellow, Color.red, Random.value);
                
                Destroy(debris[i].GetComponent<Collider>());

                // Add physics - explode outward
                var rb = debris[i].AddComponent<Rigidbody>();
                rb.mass = 0.01f;
                rb.useGravity = true;
                Vector3 explosionDir = (Random.onUnitSphere + Vector3.up * 0.5f).normalized;
                rb.AddForce(explosionDir * Random.Range(2f, 5f), ForceMode.Impulse);
                rb.AddTorque(Random.onUnitSphere * 10f);
            }

            // Create shockwave ring
            var shockwave = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            shockwave.name = "Shockwave";
            shockwave.transform.position = position;
            shockwave.transform.localScale = new Vector3(0.1f, 0.005f, 0.1f);
            
            var shockRenderer = shockwave.GetComponent<Renderer>();
            shockRenderer.material.color = new Color(1f, 0.8f, 0.4f, 0.8f);
            Destroy(shockwave.GetComponent<Collider>());

            // Animate explosion
            float elapsed = 0f;
            float explosionDuration = 1.5f;
            Vector3 fireballStartScale = fireball.transform.localScale;

            // Camera shake
            StartCoroutine(CameraShake(0.5f, 0.1f));

            while (elapsed < explosionDuration && fireball != null)
            {
                elapsed += Time.deltaTime;
                float progress = elapsed / explosionDuration;
                
                // Expand fireball then fade
                float fireballScale = 0.05f + progress * 0.4f;
                fireball.transform.localScale = Vector3.one * fireballScale;
                
                // Color transition: yellow -> orange -> red -> black
                Color fireColor;
                if (progress < 0.3f)
                    fireColor = Color.Lerp(Color.yellow, new Color(1f, 0.5f, 0f), progress / 0.3f);
                else if (progress < 0.6f)
                    fireColor = Color.Lerp(new Color(1f, 0.5f, 0f), Color.red, (progress - 0.3f) / 0.3f);
                else
                    fireColor = Color.Lerp(Color.red, new Color(0.2f, 0.1f, 0.1f), (progress - 0.6f) / 0.4f);
                
                fireColor.a = 1f - progress * 0.8f;
                fireRenderer.material.color = fireColor;
                
                // Light fades
                explosionLight.intensity = 10f * (1f - progress);
                
                // Shockwave expands
                float shockwaveScale = 0.1f + progress * 1f;
                shockwave.transform.localScale = new Vector3(shockwaveScale, 0.005f, shockwaveScale);
                Color shockColor = shockRenderer.material.color;
                shockColor.a = 0.8f * (1f - progress);
                shockRenderer.material.color = shockColor;

                yield return null;
            }

            // Cleanup
            Destroy(fireball);
            Destroy(shockwave);
            
            yield return new WaitForSeconds(1f);
            
            foreach (var d in debris)
            {
                if (d != null) Destroy(d);
            }
        }

        /// <summary>
        /// Camera shake effect for explosions
        /// </summary>
        private IEnumerator CameraShake(float duration, float magnitude)
        {
            Camera cam = Camera.main;
            if (cam == null) yield break;
            
            Vector3 originalPos = cam.transform.localPosition;
            float elapsed = 0f;

            while (elapsed < duration)
            {
                elapsed += Time.deltaTime;
                float progress = elapsed / duration;
                
                float currentMagnitude = magnitude * (1f - progress);
                float x = Random.Range(-1f, 1f) * currentMagnitude;
                float y = Random.Range(-1f, 1f) * currentMagnitude;
                
                cam.transform.localPosition = originalPos + new Vector3(x, y, 0);
                yield return null;
            }

            cam.transform.localPosition = originalPos;
        }

        /// <summary>
        /// Smoke cloud effect for after explosions
        /// </summary>
        public IEnumerator PlaySmokeEffect(Vector3 position)
        {
            Debug.Log($"[Effects] Playing smoke effect at {position}");
            
            GameObject[] smokePuffs = new GameObject[15];
            
            for (int i = 0; i < smokePuffs.Length; i++)
            {
                smokePuffs[i] = GameObject.CreatePrimitive(PrimitiveType.Sphere);
                smokePuffs[i].name = "Smoke";
                smokePuffs[i].transform.position = position + Random.insideUnitSphere * 0.1f;
                smokePuffs[i].transform.localScale = Vector3.one * Random.Range(0.03f, 0.08f);
                
                var renderer = smokePuffs[i].GetComponent<Renderer>();
                float gray = Random.Range(0.3f, 0.6f);
                renderer.material.color = new Color(gray, gray, gray, 0.5f);
                
                Destroy(smokePuffs[i].GetComponent<Collider>());
            }

            // Animate smoke rising
            float elapsed = 0f;
            float smokeDuration = 3f;

            while (elapsed < smokeDuration)
            {
                elapsed += Time.deltaTime;
                float progress = elapsed / smokeDuration;

                foreach (var puff in smokePuffs)
                {
                    if (puff != null)
                    {
                        puff.transform.position += Vector3.up * Time.deltaTime * 0.1f;
                        puff.transform.localScale *= 1.002f;
                        
                        var renderer = puff.GetComponent<Renderer>();
                        Color c = renderer.material.color;
                        c.a = 0.5f * (1f - progress);
                        renderer.material.color = c;
                    }
                }

                yield return null;
            }

            foreach (var puff in smokePuffs)
            {
                if (puff != null) Destroy(puff);
            }
        }
    }
}
