using UnityEngine;
using System.Collections;
using System.Collections.Generic;

namespace ChemistryLab.Effects
{
    using Core;
    using Data;

    /// <summary>
    /// Enhanced realistic effects for chemical reactions
    /// Uses Unity's particle system for better visuals
    /// </summary>
    public class RealisticEffects : MonoBehaviour
    {
        [Header("Settings")]
        [SerializeField] private float effectScale = 1f;
        
        private static RealisticEffects _instance;
        public static RealisticEffects Instance => _instance;

        // Audio sources for different effect types
        private AudioSource _bubbleAudio;
        private AudioSource _explosionAudio;
        private AudioSource _pourAudio;
        private AudioSource _fizzAudio;

        private void Awake()
        {
            _instance = this;
            SetupAudio();
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

        private void SetupAudio()
        {
            // Create audio sources for different sounds
            _bubbleAudio = gameObject.AddComponent<AudioSource>();
            _bubbleAudio.playOnAwake = false;
            _bubbleAudio.spatialBlend = 0.5f;
            _bubbleAudio.volume = 0.5f;
            _bubbleAudio.clip = Audio.ProceduralAudio.CreateBubbleSound();
            _bubbleAudio.loop = true;

            _explosionAudio = gameObject.AddComponent<AudioSource>();
            _explosionAudio.playOnAwake = false;
            _explosionAudio.spatialBlend = 0.3f;
            _explosionAudio.volume = 0.8f;
            _explosionAudio.clip = Audio.ProceduralAudio.CreateExplosionSound();

            _pourAudio = gameObject.AddComponent<AudioSource>();
            _pourAudio.playOnAwake = false;
            _pourAudio.spatialBlend = 0.5f;
            _pourAudio.volume = 0.4f;
            _pourAudio.clip = Audio.ProceduralAudio.CreatePourSound();
            _pourAudio.loop = true;

            _fizzAudio = gameObject.AddComponent<AudioSource>();
            _fizzAudio.playOnAwake = false;
            _fizzAudio.spatialBlend = 0.5f;
            _fizzAudio.volume = 0.4f;
            _fizzAudio.clip = Audio.ProceduralAudio.CreateFizzSound();
            _fizzAudio.loop = true;
        }

        private void OnReaction(ReactionResult result)
        {
            if (result == null || !result.isSuccessful) return;

            // Find containers to apply effects
            var containers = FindObjectsByType<Containers.ChemicalContainer>(FindObjectsSortMode.None);
            if (containers.Length == 0) return;

            var container = containers[0];
            Vector3 pos = container.transform.position + Vector3.up * 0.1f;

            // Play appropriate effects based on reaction type
            // Process reaction type
            if (result.reaction.reactionName.Contains("Magnesium"))
            {
                StartCoroutine(PlayMagnesiumFlash(pos));
                return; // Override standard flame
            }

            switch (result.primaryEffect)
            {
                case ReactionEffectType.GasEvolution:
                    StartCoroutine(PlayRealisticBubbles(pos, 3f));
                    break;
                case ReactionEffectType.Heat:
                    StartCoroutine(PlayHeatGlow(container.gameObject, result.temperatureChange));
                    StartCoroutine(PlaySteamEffect(pos));
                    break;
                case ReactionEffectType.Explosion:
                    StartCoroutine(PlayShatteringExplosion(container.gameObject));
                    break;
                case ReactionEffectType.Flame:
                    StartCoroutine(PlayProceduralFire(pos)); // NEW Particle System Fire
                    break;
                case ReactionEffectType.Precipitate:
                    StartCoroutine(PlayPrecipitateSettling(pos, result.resultColor));
                    break;
                case ReactionEffectType.ColorChange:
                    StartCoroutine(PlayGradualColorChange(container.gameObject, result.resultColor));
                    break;
                case ReactionEffectType.Fizzing:
                    StartCoroutine(PlayFizzing(pos, 2f));
                    break;
                case ReactionEffectType.Foam:
                    StartCoroutine(PlayFoamEruption(container.gameObject));
                    break;
                case ReactionEffectType.Smoke:
                    StartCoroutine(PlayProceduralSmoke(pos, result.resultColor));
                    break;
            }

            // Check secondary effects
            foreach (var effect in result.allEffects)
            {
                if (effect == result.primaryEffect) continue;
                
                if (effect == ReactionEffectType.Explosion)
                    StartCoroutine(PlayShatteringExplosion(container.gameObject));
                else if (effect == ReactionEffectType.Flame && !result.reaction.reactionName.Contains("Magnesium"))
                    StartCoroutine(PlayProceduralFire(pos));
                else if (effect == ReactionEffectType.Smoke)
                    StartCoroutine(PlayProceduralSmoke(pos, Color.white));
            }
        }

        // --- NEW PROCEDURAL PARTICLE EFFECTS ---

        /// <summary>
        /// Creates a blinding white flash with sparks using Unity ParticleSystem
        /// </summary>
        public IEnumerator PlayMagnesiumFlash(Vector3 position)
        {
            Debug.Log("[RealisticEffects] ✨ MAGNESIUM FLASH!");

            // 1. Light Flash
            GameObject lightObj = new GameObject("MgLight");
            lightObj.transform.position = position;
            Light light = lightObj.AddComponent<Light>();
            light.type = LightType.Point;
            light.color = Color.white;
            light.range = 5f;
            light.intensity = 0f;

            // 2. Particle System (Sparks)
            GameObject vfxObj = new GameObject("MgSparks");
            vfxObj.transform.position = position;
            var ps = vfxObj.AddComponent<ParticleSystem>();
            var render = vfxObj.GetComponent<ParticleSystemRenderer>();
            render.material = new Material(Shader.Find("Particles/Standard Unlit"));
            render.material.SetColor("_Color", Color.white);
            render.trailMaterial = render.material;

            var main = ps.main;
            main.startColor = Color.white;
            main.startSize = 0.05f;
            main.startSpeed = new ParticleSystem.MinMaxCurve(2f, 5f);
            main.startLifetime = 1f;
            main.simulationSpace = ParticleSystemSimulationSpace.World;
            
            var emission = ps.emission;
            emission.rateOverTime = 0;
            emission.SetBursts(new[] { new ParticleSystem.Burst(0f, 50) });

            var shape = ps.shape;
            shape.shapeType = ParticleSystemShapeType.Sphere;
            shape.radius = 0.1f;

            var trails = ps.trails;
            trails.enabled = true;
            trails.ratio = 0.5f;
            trails.widthOverTrail = 0.5f;

            // Play
            ps.Play();

            // Animate Light
            float duration = 1.5f;
            float elapsed = 0f;
            while (elapsed < duration)
            {
                elapsed += Time.deltaTime;
                float p = elapsed / duration;

                // Blinding spike then fade
                if (p < 0.1f) light.intensity = Mathf.Lerp(0, 50f, p / 0.1f);
                else light.intensity = Mathf.Lerp(50f, 0f, (p - 0.1f) / 0.9f);

                yield return null;
            }

            Destroy(lightObj);
            Destroy(vfxObj, 2f);
        }

        public IEnumerator PlayProceduralFire(Vector3 position)
        {
            GameObject vfxObj = new GameObject("ProceduralFire");
            vfxObj.transform.position = position;
            
            // Core Fire System
            var ps = vfxObj.AddComponent<ParticleSystem>();
            var renderer = vfxObj.GetComponent<ParticleSystemRenderer>();
            renderer.material = new Material(Shader.Find("Particles/Standard Unlit"));
            renderer.material.SetColor("_Color", new Color(1f, 0.5f, 0.2f));

            var main = ps.main;
            main.startColor = new ParticleSystem.MinMaxGradient(new Color(1f, 0.8f, 0.1f), new Color(1f, 0.3f, 0f));
            main.startSize = new ParticleSystem.MinMaxCurve(0.1f, 0.3f);
            main.startLifetime = 1f;
            main.startSpeed = 1f;
            main.loop = false;
            main.duration = 3f;

            var emission = ps.emission;
            emission.rateOverTime = 20f;

            var shape = ps.shape;
            shape.angle = 15f;
            shape.shapeType = ParticleSystemShapeType.Cone;
            
             // Size over lifetime (shrink)
            var sol = ps.sizeOverLifetime;
            sol.enabled = true;
            sol.size = new ParticleSystem.MinMaxCurve(1f, 0f);

            ps.Play();
            
            // Add light
            GameObject lightObj = new GameObject("FireLight");
            lightObj.transform.parent = vfxObj.transform;
            lightObj.transform.localPosition = Vector3.zero;
            Light light = lightObj.AddComponent<Light>();
            light.color = new Color(1f, 0.6f, 0.2f);
            light.intensity = 2f;
            
             StartCoroutine(PlayHeatGlow(lightObj, 2f)); // Re-use heat glow for flicker

            yield return new WaitForSeconds(4f);
            Destroy(vfxObj);
        }

        public IEnumerator PlayProceduralSmoke(Vector3 position, Color smokeColor)
        {
             GameObject vfxObj = new GameObject("ProceduralSmoke");
            vfxObj.transform.position = position;
            
            var ps = vfxObj.AddComponent<ParticleSystem>();
            var renderer = vfxObj.GetComponent<ParticleSystemRenderer>();
            renderer.material = new Material(Shader.Find("Particles/Standard Unlit"));
            renderer.material.SetColor("_Color", smokeColor);

            var main = ps.main;
            main.startColor = new Color(smokeColor.r, smokeColor.g, smokeColor.b, 0.3f);
            main.startSize = new ParticleSystem.MinMaxCurve(0.2f, 0.5f);
            main.startLifetime = 3f;
            main.startSpeed = 0.5f;

            var emission = ps.emission;
            emission.rateOverTime = 10f;

            var sol = ps.sizeOverLifetime;
            sol.enabled = true;
            sol.size = new ParticleSystem.MinMaxCurve(0.5f, 2f); // Expand

            ps.Play();
            yield return new WaitForSeconds(5f);
            Destroy(vfxObj);
        }

        /// <summary>
        /// Massive foam eruption effect (Elephant's Toothpaste)
        /// </summary>
        public IEnumerator PlayFoamEruption(GameObject container)
        {
            Debug.Log("[RealisticEffects] 🐘 ELEPHANT'S TOOTHPASTE FOAM ERUPTION!");
            Vector3 startPos = container.transform.position + Vector3.up * 0.1f;
            
            // Play intense fizz/bubbling sound
            if (_fizzAudio != null)
            {
                _fizzAudio.transform.position = startPos;
                _fizzAudio.Play();
            }

            // Create rising column of foam
            List<GameObject> foamBubbles = new List<GameObject>();
            float duration = 10f; // Long duration
            float elapsed = 0f;
            int bubblesPerFrame = 2; // High density

            while (elapsed < duration)
            {
                elapsed += Time.deltaTime;

                // Spawn foam particles
                for (int i = 0; i < bubblesPerFrame; i++)
                {
                    var bubble = GameObject.CreatePrimitive(PrimitiveType.Sphere);
                    bubble.name = "FoamBubble";
                    // Spawn slightly above container
                    bubble.transform.position = startPos + Random.insideUnitSphere * 0.05f;
                    
                    // Varied sizes
                    bubble.transform.localScale = Vector3.one * Random.Range(0.08f, 0.15f);
                    
                    Destroy(bubble.GetComponent<Collider>()); // No collision initially to prevent immediate explosion
                    
                    // Add physics after a frame? Or just use sphere collider
                    // Actually we want them to pile up. So we DO need physics.
                    // But if they spawn inside each other, they might explode.
                    // Let's spawn them moving UP.
                    
                    var rb = bubble.AddComponent<Rigidbody>();
                    rb.mass = 0.05f;
                    rb.linearDamping = 2f; // Foam is viscous
                    rb.AddForce(Vector3.up * Random.Range(1f, 3f), ForceMode.Impulse);
                    rb.AddForce(Random.insideUnitSphere * 0.5f, ForceMode.Impulse);

                    // Re-add collider
                    var col = bubble.AddComponent<SphereCollider>();
                    col.material = new PhysicsMaterial { bounciness = 0f, dynamicFriction = 0.5f, staticFriction = 0.5f };

                    // Color - striped light blue/white
                    var renderer = bubble.GetComponent<Renderer>();
                    bool isStripe = Random.value > 0.7f;
                    renderer.material.color = isStripe ? new Color(0.6f, 0.8f, 1f) : Color.white;
                    
                    foamBubbles.Add(bubble);
                    
                    // Limit total bubbles
                    if (foamBubbles.Count > 300)
                    {
                        var old = foamBubbles[0];
                        foamBubbles.RemoveAt(0);
                        Destroy(old);
                    }
                }
                
                // Camera shake initially
                if (elapsed < 3f)
                    StartCoroutine(CameraShake(0.1f, 0.05f));

                yield return null;
            }

            // Cleanup
            if (_fizzAudio != null) _fizzAudio.Stop();
            
            // Fade out foam
            yield return new WaitForSeconds(2f);
            
            foreach (var b in foamBubbles)
            {
                if (b != null)
                {
                    Destroy(b.GetComponent<Collider>());
                    Destroy(b, Random.Range(0f, 2f)); // Random fade
                }
            }
        }

        /// <summary>
        /// Realistic bubbling effect with varied sizes and speeds
        /// </summary>
        public IEnumerator PlayRealisticBubbles(Vector3 position, float duration)
        {
            Debug.Log("[RealisticEffects] Playing bubble effect");
            
            // Play audio
            if (_bubbleAudio != null)
            {
                _bubbleAudio.transform.position = position;
                _bubbleAudio.Play();
            }
            
            float elapsed = 0f;
            List<GameObject> bubbles = new List<GameObject>();

            while (elapsed < duration)
            {
                elapsed += Time.deltaTime;
                
                // Spawn bubbles at varying rates
                if (Random.value > 0.7f)
                {
                    var bubble = CreateBubble(position);
                    bubbles.Add(bubble);
                    StartCoroutine(AnimateBubble(bubble));
                }

                yield return null;
            }

            if (_bubbleAudio != null) _bubbleAudio.Stop();

            // Cleanup any remaining bubbles
            yield return new WaitForSeconds(2f);
            foreach (var b in bubbles)
            {
                if (b != null) Destroy(b);
            }
        }

        public IEnumerator PlayFizzing(Vector3 position, float duration)
        {
            // Play audio
            if (_fizzAudio != null)
            {
                _fizzAudio.transform.position = position;
                _fizzAudio.Play();
            }

            // Create tiny rapid bubbles for fizzing visual
            float elapsed = 0f;
            List<GameObject> bubbles = new List<GameObject>();

            while (elapsed < duration)
            {
                elapsed += Time.deltaTime;
                
                // Spawn many tiny bubbles
                if (Random.value > 0.3f)
                {
                    float radius = 0.05f; // Small radius variation
                    Vector3 randPos = position + Random.insideUnitSphere * radius;
                    var bubble = CreateBubble(randPos);
                    bubble.transform.localScale *= 0.3f; // Tiny!
                    bubbles.Add(bubble);
                    StartCoroutine(AnimateBubble(bubble));
                }

                yield return null;
            }

            // Stop audio
            if (_fizzAudio != null) _fizzAudio.Stop();
            
            // Cleanup
            yield return new WaitForSeconds(1f);
            foreach (var b in bubbles) if(b != null) Destroy(b);
        }

        public void PlayPouringSound(Vector3 position, bool isPouring)
        {
            if (_pourAudio == null) return;
            
            _pourAudio.transform.position = position;
            
            if (isPouring)
            {
                if (!_pourAudio.isPlaying) _pourAudio.Play();
            }
            else
            {
                if (_pourAudio.isPlaying) _pourAudio.Stop();
            }
        }

        private GameObject CreateBubble(Vector3 basePos)
        {
            var bubble = GameObject.CreatePrimitive(PrimitiveType.Sphere);
            bubble.name = "RealisticBubble";
            
            float size = Random.Range(0.005f, 0.02f) * effectScale;
            bubble.transform.localScale = Vector3.one * size;
            bubble.transform.position = basePos + new Vector3(
                Random.Range(-0.03f, 0.03f),
                Random.Range(-0.02f, 0.02f),
                Random.Range(-0.03f, 0.03f)
            );

            Destroy(bubble.GetComponent<Collider>());

            var renderer = bubble.GetComponent<Renderer>();
            var mat = renderer.material;
            mat.color = new Color(0.9f, 0.95f, 1f, 0.6f);
            
            // Make it slightly shiny/reflective
            mat.SetFloat("_Smoothness", 0.9f);

            return bubble;
        }

        private IEnumerator AnimateBubble(GameObject bubble)
        {
            if (bubble == null) yield break;

            float lifetime = Random.Range(0.8f, 2f);
            float speed = Random.Range(0.08f, 0.2f);
            float wobbleSpeed = Random.Range(3f, 8f);
            float wobbleAmount = Random.Range(0.005f, 0.015f);
            Vector3 startPos = bubble.transform.position;
            float elapsed = 0f;

            while (elapsed < lifetime && bubble != null)
            {
                elapsed += Time.deltaTime;
                float progress = elapsed / lifetime;

                // Rise with wobble
                float xWobble = Mathf.Sin(elapsed * wobbleSpeed) * wobbleAmount;
                float zWobble = Mathf.Cos(elapsed * wobbleSpeed * 0.8f) * wobbleAmount;
                
                bubble.transform.position = startPos + new Vector3(xWobble, elapsed * speed, zWobble);

                // Grow slightly as it rises (like real bubbles)
                float scale = bubble.transform.localScale.x * (1f + progress * 0.3f);
                bubble.transform.localScale = Vector3.one * scale;

                // Fade out near end
                if (progress > 0.7f)
                {
                    var renderer = bubble.GetComponent<Renderer>();
                    if (renderer != null)
                    {
                        Color c = renderer.material.color;
                        c.a = 0.6f * (1f - (progress - 0.7f) / 0.3f);
                        renderer.material.color = c;
                    }
                }

                yield return null;
            }

            if (bubble != null) Destroy(bubble);
        }

        /// <summary>
        /// Heat glow effect - makes container glow when hot
        /// </summary>
        public IEnumerator PlayHeatGlow(GameObject container, float intensity)
        {
            Debug.Log("[RealisticEffects] Playing heat glow");
            
            // Create a glowing point light
            var glowLight = new GameObject("HeatGlow");
            glowLight.transform.SetParent(container.transform);
            glowLight.transform.localPosition = Vector3.zero;

            var light = glowLight.AddComponent<Light>();
            light.type = LightType.Point;
            light.color = new Color(1f, 0.4f, 0.1f); // Orange-red
            light.range = 0.3f;
            light.intensity = 0f;

            // Pulse the glow
            float duration = 5f;
            float elapsed = 0f;

            while (elapsed < duration)
            {
                elapsed += Time.deltaTime;
                float progress = elapsed / duration;

                // Fade in, pulse, fade out
                if (progress < 0.2f)
                    light.intensity = Mathf.Lerp(0, 2f, progress / 0.2f);
                else if (progress < 0.8f)
                    light.intensity = 2f + Mathf.Sin(elapsed * 5f) * 0.5f; // Pulse
                else
                    light.intensity = Mathf.Lerp(2f, 0, (progress - 0.8f) / 0.2f);

                yield return null;
            }

            Destroy(glowLight);
        }

        /// <summary>
        /// Steam effect rising from hot liquid
        /// </summary>
        public IEnumerator PlaySteamEffect(Vector3 position)
        {
            Debug.Log("[RealisticEffects] Playing steam effect");
            
            float duration = 4f;
            float elapsed = 0f;

            while (elapsed < duration)
            {
                elapsed += Time.deltaTime;

                if (Random.value > 0.8f)
                {
                    var steam = CreateSteamPuff(position);
                    StartCoroutine(AnimateSteam(steam));
                }

                yield return null;
            }
        }

        private GameObject CreateSteamPuff(Vector3 basePos)
        {
            var puff = GameObject.CreatePrimitive(PrimitiveType.Sphere);
            puff.name = "SteamPuff";
            
            float size = Random.Range(0.02f, 0.05f) * effectScale;
            puff.transform.localScale = Vector3.one * size;
            puff.transform.position = basePos + new Vector3(
                Random.Range(-0.02f, 0.02f), 0, Random.Range(-0.02f, 0.02f));

            Destroy(puff.GetComponent<Collider>());

            var renderer = puff.GetComponent<Renderer>();
            renderer.material.color = new Color(0.95f, 0.95f, 1f, 0.25f);

            return puff;
        }

        private IEnumerator AnimateSteam(GameObject steam)
        {
            if (steam == null) yield break;

            float lifetime = Random.Range(1.5f, 3f);
            float elapsed = 0f;
            Vector3 startPos = steam.transform.position;
            Vector3 startScale = steam.transform.localScale;
            float drift = Random.Range(-0.03f, 0.03f);

            while (elapsed < lifetime && steam != null)
            {
                elapsed += Time.deltaTime;
                float progress = elapsed / lifetime;

                // Rise and expand
                steam.transform.position = startPos + new Vector3(
                    drift * progress,
                    elapsed * 0.15f,
                    drift * progress * 0.5f
                );
                steam.transform.localScale = startScale * (1f + progress * 2f);

                // Fade out
                var renderer = steam.GetComponent<Renderer>();
                if (renderer != null)
                {
                    Color c = renderer.material.color;
                    c.a = 0.25f * (1f - progress);
                    renderer.material.color = c;
                }

                yield return null;
            }

            if (steam != null) Destroy(steam);
        }

        /// <summary>
        /// Shattering explosion - creates glass shards and destroys the container
        /// </summary>
        public IEnumerator PlayShatteringExplosion(GameObject container)
        {
            if (container == null) yield break;

            Vector3 position = container.transform.position;
            Debug.Log("[RealisticEffects] 💥 SHATTERING EXPLOSION - Container will be destroyed!");

            // Play explosion sound
            if (_explosionAudio != null)
            {
                _explosionAudio.transform.position = position;
                _explosionAudio.Play();
            }

            // Camera shake - intense
            StartCoroutine(CameraShake(0.8f, 0.2f));

            // Hide container immediately
            container.SetActive(false);

            // Create central flash (bright white)
            var flash = GameObject.CreatePrimitive(PrimitiveType.Sphere);
            flash.name = "ExplosionFlash";
            flash.transform.position = position;
            flash.transform.localScale = Vector3.one * 0.5f;
            Destroy(flash.GetComponent<Collider>());
            var flashRenderer = flash.GetComponent<Renderer>();
            flashRenderer.material.color = new Color(1f, 0.9f, 0.7f, 1f);
            flashRenderer.material.EnableKeyword("_EMISSION");
            flashRenderer.material.SetColor("_EmissionColor", Color.white * 3f);

            // Create glass shards flying outward
            List<GameObject> shards = new List<GameObject>();
            Color glassColor = new Color(0.7f, 0.85f, 1f, 0.6f); // Light blue glass
            
            for (int i = 0; i < 25; i++)
            {
                var shard = GameObject.CreatePrimitive(PrimitiveType.Cube);
                shard.name = "GlassShard";
                shard.transform.position = position + Random.insideUnitSphere * 0.05f;
                
                // Random shard shape - thin and elongated
                float length = Random.Range(0.02f, 0.06f);
                float width = Random.Range(0.005f, 0.02f);
                shard.transform.localScale = new Vector3(width, length, width);
                shard.transform.rotation = Random.rotation;
                
                Destroy(shard.GetComponent<Collider>());
                
                // Glass material
                var shardRenderer = shard.GetComponent<Renderer>();
                var mat = new Material(Shader.Find("Standard"));
                mat.SetFloat("_Mode", 3);
                mat.SetInt("_SrcBlend", (int)UnityEngine.Rendering.BlendMode.SrcAlpha);
                mat.SetInt("_DstBlend", (int)UnityEngine.Rendering.BlendMode.OneMinusSrcAlpha);
                mat.EnableKeyword("_ALPHABLEND_ON");
                mat.color = glassColor;
                mat.renderQueue = 3000;
                shardRenderer.material = mat;
                
                // Add rigidbody for physics
                var rb = shard.AddComponent<Rigidbody>();
                rb.mass = 0.01f;
                rb.useGravity = true;
                
                // Explode outward
                Vector3 explosionDir = (shard.transform.position - position).normalized;
                if (explosionDir.magnitude < 0.1f)
                    explosionDir = Random.onUnitSphere;
                rb.AddForce(explosionDir * Random.Range(3f, 8f), ForceMode.Impulse);
                rb.AddTorque(Random.insideUnitSphere * 10f, ForceMode.Impulse);
                
                shards.Add(shard);
            }

            // Create fire/smoke particles
            List<GameObject> fireParticles = new List<GameObject>();
            for (int i = 0; i < 15; i++)
            {
                var fire = GameObject.CreatePrimitive(PrimitiveType.Sphere);
                fire.name = "FireParticle";
                fire.transform.position = position + Random.insideUnitSphere * 0.1f;
                fire.transform.localScale = Vector3.one * Random.Range(0.05f, 0.15f);
                Destroy(fire.GetComponent<Collider>());
                
                var fireRenderer = fire.GetComponent<Renderer>();
                Color fireColor = Color.Lerp(new Color(1f, 0.5f, 0f), new Color(1f, 0.2f, 0f), Random.value);
                fireRenderer.material.color = fireColor;
                fireRenderer.material.EnableKeyword("_EMISSION");
                fireRenderer.material.SetColor("_EmissionColor", fireColor * 2f);
                
                fireParticles.Add(fire);
            }

            // Animate
            float elapsed = 0f;
            float duration = 2f;

            while (elapsed < duration)
            {
                elapsed += Time.deltaTime;
                float progress = elapsed / duration;

                // Flash fades quickly
                if (flash != null)
                {
                    float flashAlpha = 1f - (progress * 5f);
                    if (flashAlpha <= 0)
                    {
                        Destroy(flash);
                        flash = null;
                    }
                    else
                    {
                        flash.transform.localScale = Vector3.one * (0.5f + progress * 2f);
                        var r = flash.GetComponent<Renderer>();
                        Color c = r.material.color;
                        c.a = flashAlpha;
                        r.material.color = c;
                    }
                }

                // Fire particles rise and fade
                foreach (var fire in fireParticles)
                {
                    if (fire != null)
                    {
                        fire.transform.position += Vector3.up * Time.deltaTime * 0.3f;
                        fire.transform.localScale *= 0.98f;
                        var r = fire.GetComponent<Renderer>();
                        Color c = r.material.color;
                        c.a = 1f - progress;
                        r.material.color = c;
                    }
                }

                yield return null;
            }

            // Cleanup
            if (flash != null) Destroy(flash);
            foreach (var fire in fireParticles)
                if (fire != null) Destroy(fire);

            // Wait a bit then clean up shards
            yield return new WaitForSeconds(1.5f);
            
            foreach (var shard in shards)
                if (shard != null) Destroy(shard);

            // DESTROY THE CONTAINER permanently
            if (container != null)
            {
                Destroy(container);
                Debug.Log("[RealisticEffects] Container destroyed!");
            }
        }

        /// <summary>
        /// Realistic explosion with shockwave and debris
        /// </summary>
        public IEnumerator PlayRealisticExplosion(Vector3 position)
        {
            Debug.Log("[RealisticEffects] 💥 REALISTIC EXPLOSION!");

            // Camera shake
            StartCoroutine(CameraShake(0.6f, 0.15f));

            // Create central flash
            var flash = CreateExplosionFlash(position);
            
            // Create shockwave
            var shockwave = CreateShockwave(position);
            
            // Create debris particles
            List<GameObject> debris = new List<GameObject>();
            for (int i = 0; i < 40; i++)
            {
                debris.Add(CreateDebris(position));
            }

            // Create fireball
            var fireball = CreateFireball(position);

            // Animate everything
            float elapsed = 0f;
            float duration = 2f;

            while (elapsed < duration)
            {
                elapsed += Time.deltaTime;
                float progress = elapsed / duration;

                // Flash fades quickly
                if (flash != null && progress < 0.1f)
                {
                    UpdateFlash(flash, progress / 0.1f);
                }
                else if (flash != null)
                {
                    Destroy(flash);
                    flash = null;
                }

                // Shockwave expands
                if (shockwave != null)
                {
                    UpdateShockwave(shockwave, progress);
                    if (progress > 0.5f) 
                    {
                        Destroy(shockwave);
                        shockwave = null;
                    }
                }

                // Fireball grows then fades
                if (fireball != null)
                {
                    UpdateFireball(fireball, progress);
                    if (progress > 0.6f)
                    {
                        Destroy(fireball);
                        fireball = null;
                    }
                }

                yield return null;
            }

            // Cleanup
            if (flash != null) Destroy(flash);
            if (shockwave != null) Destroy(shockwave);
            if (fireball != null) Destroy(fireball);
            
            yield return new WaitForSeconds(1f);
            foreach (var d in debris)
            {
                if (d != null) Destroy(d);
            }
        }

        private GameObject CreateExplosionFlash(Vector3 pos)
        {
            var flash = GameObject.CreatePrimitive(PrimitiveType.Sphere);
            flash.name = "ExplosionFlash";
            flash.transform.position = pos;
            flash.transform.localScale = Vector3.one * 0.1f * effectScale;
            Destroy(flash.GetComponent<Collider>());

            var renderer = flash.GetComponent<Renderer>();
            renderer.material.color = Color.white;

            var light = flash.AddComponent<Light>();
            light.type = LightType.Point;
            light.color = new Color(1f, 0.9f, 0.7f);
            light.intensity = 15f;
            light.range = 3f;

            return flash;
        }

        private void UpdateFlash(GameObject flash, float progress)
        {
            float scale = 0.1f + progress * 0.5f;
            flash.transform.localScale = Vector3.one * scale * effectScale;
            
            var light = flash.GetComponent<Light>();
            if (light != null)
            {
                light.intensity = 15f * (1f - progress);
            }
        }

        private GameObject CreateShockwave(Vector3 pos)
        {
            var wave = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            wave.name = "Shockwave";
            wave.transform.position = pos;
            wave.transform.localScale = new Vector3(0.1f, 0.01f, 0.1f) * effectScale;
            Destroy(wave.GetComponent<Collider>());

            var renderer = wave.GetComponent<Renderer>();
            renderer.material.color = new Color(1f, 0.8f, 0.5f, 0.7f);

            return wave;
        }

        private void UpdateShockwave(GameObject wave, float progress)
        {
            float scale = (0.1f + progress * 2f) * effectScale;
            wave.transform.localScale = new Vector3(scale, 0.01f, scale);

            var renderer = wave.GetComponent<Renderer>();
            Color c = renderer.material.color;
            c.a = 0.7f * (1f - progress * 2f);
            renderer.material.color = c;
        }

        private GameObject CreateFireball(Vector3 pos)
        {
            var ball = GameObject.CreatePrimitive(PrimitiveType.Sphere);
            ball.name = "Fireball";
            ball.transform.position = pos;
            ball.transform.localScale = Vector3.one * 0.05f * effectScale;
            Destroy(ball.GetComponent<Collider>());

            var renderer = ball.GetComponent<Renderer>();
            renderer.material.color = Color.yellow;

            var light = ball.AddComponent<Light>();
            light.type = LightType.Point;
            light.color = new Color(1f, 0.6f, 0.2f);
            light.intensity = 8f;
            light.range = 1.5f;

            return ball;
        }

        private void UpdateFireball(GameObject ball, float progress)
        {
            // Expand then shrink
            float scale;
            if (progress < 0.3f)
                scale = Mathf.Lerp(0.05f, 0.4f, progress / 0.3f);
            else
                scale = Mathf.Lerp(0.4f, 0.1f, (progress - 0.3f) / 0.3f);
            
            ball.transform.localScale = Vector3.one * scale * effectScale;

            // Color transition: yellow -> orange -> red -> dark
            var renderer = ball.GetComponent<Renderer>();
            Color c;
            if (progress < 0.2f)
                c = Color.Lerp(Color.white, Color.yellow, progress / 0.2f);
            else if (progress < 0.4f)
                c = Color.Lerp(Color.yellow, new Color(1f, 0.5f, 0f), (progress - 0.2f) / 0.2f);
            else
                c = Color.Lerp(new Color(1f, 0.5f, 0f), new Color(0.3f, 0.1f, 0f), (progress - 0.4f) / 0.2f);
            
            renderer.material.color = c;

            var light = ball.GetComponent<Light>();
            if (light != null)
            {
                light.intensity = 8f * (1f - progress);
            }
        }

        private GameObject CreateDebris(Vector3 pos)
        {
            bool isSphere = Random.value > 0.5f;
            var debris = GameObject.CreatePrimitive(isSphere ? PrimitiveType.Sphere : PrimitiveType.Cube);
            debris.name = "Debris";
            debris.transform.position = pos;
            debris.transform.localScale = Vector3.one * Random.Range(0.01f, 0.03f) * effectScale;
            debris.transform.rotation = Random.rotation;

            Destroy(debris.GetComponent<Collider>());

            var renderer = debris.GetComponent<Renderer>();
            renderer.material.color = Color.Lerp(Color.yellow, new Color(0.3f, 0.15f, 0f), Random.value);

            var rb = debris.AddComponent<Rigidbody>();
            rb.mass = 0.01f;
            rb.useGravity = true;
            
            Vector3 dir = (Random.onUnitSphere + Vector3.up * 0.5f).normalized;
            rb.AddForce(dir * Random.Range(3f, 8f), ForceMode.Impulse);
            rb.AddTorque(Random.onUnitSphere * 10f);

            return debris;
        }

        /// <summary>
        /// Realistic fire effect
        /// </summary>
        public IEnumerator PlayRealisticFire(Vector3 position)
        {
            Debug.Log("[RealisticEffects] 🔥 Playing fire effect");

            float duration = 3f;
            float elapsed = 0f;

            // Add fire light
            var fireLight = new GameObject("FireLight");
            fireLight.transform.position = position;
            var light = fireLight.AddComponent<Light>();
            light.type = LightType.Point;
            light.color = new Color(1f, 0.5f, 0.1f);
            light.intensity = 3f;
            light.range = 0.8f;

            while (elapsed < duration)
            {
                elapsed += Time.deltaTime;
                float progress = elapsed / duration;

                // Flicker the light
                light.intensity = 3f + Mathf.Sin(elapsed * 15f) * 1f + Mathf.Sin(elapsed * 23f) * 0.5f;

                // Spawn flame particles
                if (Random.value > 0.6f)
                {
                    var flame = CreateFlameParticle(position);
                    StartCoroutine(AnimateFlame(flame));
                }

                // Fade out near end
                if (progress > 0.7f)
                {
                    light.intensity *= (1f - (progress - 0.7f) / 0.3f);
                }

                yield return null;
            }

            Destroy(fireLight);
        }

        private GameObject CreateFlameParticle(Vector3 basePos)
        {
            var flame = GameObject.CreatePrimitive(PrimitiveType.Sphere);
            flame.name = "Flame";
            flame.transform.position = basePos + new Vector3(
                Random.Range(-0.02f, 0.02f), 0, Random.Range(-0.02f, 0.02f));
            flame.transform.localScale = Vector3.one * Random.Range(0.02f, 0.05f) * effectScale;
            
            Destroy(flame.GetComponent<Collider>());

            var renderer = flame.GetComponent<Renderer>();
            renderer.material.color = Color.Lerp(Color.yellow, new Color(1f, 0.3f, 0f), Random.value);

            return flame;
        }

        private IEnumerator AnimateFlame(GameObject flame)
        {
            if (flame == null) yield break;

            float lifetime = Random.Range(0.3f, 0.7f);
            float elapsed = 0f;
            Vector3 startPos = flame.transform.position;

            while (elapsed < lifetime && flame != null)
            {
                elapsed += Time.deltaTime;
                float progress = elapsed / lifetime;

                flame.transform.position = startPos + Vector3.up * elapsed * 0.4f +
                    new Vector3(Mathf.Sin(elapsed * 12f) * 0.01f, 0, 0);
                flame.transform.localScale *= 0.97f;

                var renderer = flame.GetComponent<Renderer>();
                renderer.material.color = Color.Lerp(Color.yellow, Color.red, progress);

                yield return null;
            }

            if (flame != null) Destroy(flame);
        }

        /// <summary>
        /// Precipitate settling effect
        /// </summary>
        public IEnumerator PlayPrecipitateSettling(Vector3 position, Color precipitateColor)
        {
            Debug.Log("[RealisticEffects] Playing precipitate settling");

            List<GameObject> particles = new List<GameObject>();
            
            // Create particles at top, let them settle
            for (int i = 0; i < 30; i++)
            {
                var p = GameObject.CreatePrimitive(PrimitiveType.Sphere);
                p.name = "Precipitate";
                p.transform.position = position + new Vector3(
                    Random.Range(-0.03f, 0.03f),
                    Random.Range(0.02f, 0.08f),
                    Random.Range(-0.03f, 0.03f)
                );
                p.transform.localScale = Vector3.one * Random.Range(0.003f, 0.01f) * effectScale;
                
                Destroy(p.GetComponent<Collider>());

                var renderer = p.GetComponent<Renderer>();
                Color c = precipitateColor.a > 0.1f ? precipitateColor : Color.white;
                c.a = 0.9f;
                renderer.material.color = c;

                particles.Add(p);
            }

            // Animate settling
            float duration = 3f;
            float elapsed = 0f;

            while (elapsed < duration)
            {
                elapsed += Time.deltaTime;

                foreach (var p in particles)
                {
                    if (p != null && p.transform.position.y > position.y - 0.05f)
                    {
                        p.transform.position += Vector3.down * Time.deltaTime * 0.02f;
                        // Add slight wobble
                        p.transform.position += new Vector3(
                            Mathf.Sin(elapsed * 3f + p.transform.position.x * 100) * 0.0002f,
                            0,
                            Mathf.Cos(elapsed * 3f + p.transform.position.z * 100) * 0.0002f
                        );
                    }
                }

                yield return null;
            }

            // Leave precipitate visible for a while, then cleanup
            yield return new WaitForSeconds(5f);
            foreach (var p in particles)
            {
                if (p != null) Destroy(p);
            }
        }

        /// <summary>
        /// Gradual color change animation
        /// </summary>
        public IEnumerator PlayGradualColorChange(GameObject container, Color targetColor)
        {
            Debug.Log("[RealisticEffects] Playing gradual color change");

            // Find renderers in container
            var renderers = container.GetComponentsInChildren<Renderer>();
            if (renderers.Length == 0) yield break;

            // Get starting colors
            Dictionary<Renderer, Color> startColors = new Dictionary<Renderer, Color>();
            foreach (var r in renderers)
            {
                startColors[r] = r.material.color;
            }

            float duration = 2f;
            float elapsed = 0f;

            while (elapsed < duration)
            {
                elapsed += Time.deltaTime;
                float progress = elapsed / duration;
                
                // Smooth interpolation
                float smoothProgress = Mathf.SmoothStep(0, 1, progress);

                foreach (var r in renderers)
                {
                    if (r != null && startColors.ContainsKey(r))
                    {
                        r.material.color = Color.Lerp(startColors[r], targetColor, smoothProgress);
                    }
                }

                yield return null;
            }
        }

        /// <summary>
        /// Camera shake effect
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
                float currentMag = magnitude * (1f - progress);

                cam.transform.localPosition = originalPos + new Vector3(
                    Random.Range(-1f, 1f) * currentMag,
                    Random.Range(-1f, 1f) * currentMag,
                    0
                );

                yield return null;
            }

            cam.transform.localPosition = originalPos;
        }

        /// <summary>
        /// Play pouring sound effect
        /// </summary>
        public void PlayPourSound()
        {
            // Generate procedural pour sound
            if (_pourAudio != null)
            {
                // In a full implementation, you'd load an AudioClip here
                Debug.Log("[RealisticEffects] 🔊 Pour sound");
            }
        }

        /// <summary>
        /// Play bubbling sound
        /// </summary>
        public void PlayBubbleSound()
        {
            if (_bubbleAudio != null)
            {
                Debug.Log("[RealisticEffects] 🔊 Bubble sound");
            }
        }
    }
}
