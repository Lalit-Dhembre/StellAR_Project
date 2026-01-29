using UnityEngine;

namespace ChemistryLab.Effects
{
    using Data;
    using Core;

    /// <summary>
    /// Controls particle effects for chemical reactions
    /// </summary>
    public class ReactionEffects : MonoBehaviour
    {
        [Header("Effect Prefabs")]
        [SerializeField] private ParticleSystem precipitatePrefab;
        [SerializeField] private ParticleSystem bubblesPrefab;
        [SerializeField] private ParticleSystem vaporPrefab;
        [SerializeField] private ParticleSystem flamePrefab;
        [SerializeField] private ParticleSystem sparksPrefab;

        [Header("Pool Settings")]
        [SerializeField] private int poolSize = 5;

        private ParticleSystem[] _precipitatePool;
        private ParticleSystem[] _bubblesPool;
        private ParticleSystem[] _vaporPool;

        private void Awake()
        {
            InitializePools();
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

        private void InitializePools()
        {
            _precipitatePool = CreatePool(precipitatePrefab, "PrecipitatePool");
            _bubblesPool = CreatePool(bubblesPrefab, "BubblesPool");
            _vaporPool = CreatePool(vaporPrefab, "VaporPool");
        }

        private ParticleSystem[] CreatePool(ParticleSystem prefab, string name)
        {
            if (prefab == null) return new ParticleSystem[0];

            var pool = new ParticleSystem[poolSize];
            var parent = new GameObject(name).transform;
            parent.SetParent(transform);

            for (int i = 0; i < poolSize; i++)
            {
                pool[i] = Instantiate(prefab, parent);
                pool[i].gameObject.SetActive(false);
            }

            return pool;
        }

        private void OnReaction(ReactionResult result)
        {
            if (result == null || !result.isSuccessful) return;

            foreach (var effect in result.allEffects)
            {
                PlayEffect(effect, result.resultColor);
            }
        }

        /// <summary>
        /// Play a reaction effect at the specified position
        /// </summary>
        public void PlayEffect(ReactionEffectType type, Color color, Vector3? position = null)
        {
            Vector3 pos = position ?? transform.position;

            switch (type)
            {
                case ReactionEffectType.Precipitate:
                    PlayFromPool(_precipitatePool, pos, color);
                    break;
                case ReactionEffectType.GasEvolution:
                    PlayFromPool(_bubblesPool, pos, Color.white);
                    break;
                case ReactionEffectType.Heat:
                    PlayFromPool(_vaporPool, pos, new Color(1f, 1f, 1f, 0.5f));
                    break;
                case ReactionEffectType.ColorChange:
                    // Color change is handled by container visuals
                    break;
            }
        }

        private void PlayFromPool(ParticleSystem[] pool, Vector3 position, Color color)
        {
            if (pool == null || pool.Length == 0) return;

            foreach (var ps in pool)
            {
                if (ps != null && !ps.isPlaying)
                {
                    ps.transform.position = position;
                    ps.gameObject.SetActive(true);
                    
                    var main = ps.main;
                    main.startColor = color;
                    
                    ps.Play();
                    return;
                }
            }
        }

        /// <summary>
        /// Create precipitate particles at position
        /// </summary>
        public void PlayPrecipitate(Vector3 position, Color color)
        {
            PlayFromPool(_precipitatePool, position, color);
        }

        /// <summary>
        /// Create gas bubble effect at position
        /// </summary>
        public void PlayBubbles(Vector3 position)
        {
            PlayFromPool(_bubblesPool, position, Color.white);
        }

        /// <summary>
        /// Create vapor/steam effect at position
        /// </summary>
        public void PlayVapor(Vector3 position)
        {
            PlayFromPool(_vaporPool, position, new Color(1f, 1f, 1f, 0.3f));
        }
    }
}
