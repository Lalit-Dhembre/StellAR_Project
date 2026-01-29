using UnityEngine;

namespace ChemistryLab.Containers
{
    /// <summary>
    /// Test tube container - tall, narrow, for small amounts
    /// </summary>
    public class TestTube : ChemicalContainer
    {
        [Header("Test Tube Specific")]
        [SerializeField] private Transform rimPoint;
        [SerializeField] private ParticleSystem pourParticles;
        [SerializeField] private float pourRate = 10f; // mL per second

        private bool _isPouring = false;
        private ChemicalContainer _pourTarget;

        protected override void Start()
        {
            base.Start();
            containerName = "Test Tube";
            maxCapacity = 25f; // 25mL typical test tube
            canPour = true;
            pourAngleThreshold = 60f;
        }

        protected override void Update()
        {
            base.Update();

            // Handle pouring
            if (ShouldPour() && !IsEmpty)
            {
                if (!_isPouring)
                {
                    StartPour();
                }
                ContinuePour();
            }
            else if (_isPouring)
            {
                StopPour();
            }
        }

        private void StartPour()
        {
            _isPouring = true;
            if (pourParticles != null)
            {
                var main = pourParticles.main;
                main.startColor = currentColor.a > 0.01f ? currentColor : Color.white;
                pourParticles.Play();
            }
        }

        private void ContinuePour()
        {
            // Find nearby container to pour into
            if (_pourTarget == null)
            {
                _pourTarget = FindNearbyContainer();
            }

            if (_pourTarget != null)
            {
                float amountToPour = pourRate * Time.deltaTime;
                PourInto(_pourTarget, amountToPour);
            }
        }

        private void StopPour()
        {
            _isPouring = false;
            _pourTarget = null;
            if (pourParticles != null)
            {
                pourParticles.Stop();
            }
        }

        private ChemicalContainer FindNearbyContainer()
        {
            Vector3 pourDirection = rimPoint != null ? -rimPoint.up : -transform.up;
            Vector3 origin = rimPoint != null ? rimPoint.position : transform.position;

            RaycastHit[] hits = Physics.RaycastAll(origin, pourDirection, 0.5f);
            foreach (var hit in hits)
            {
                var container = hit.collider.GetComponent<ChemicalContainer>();
                if (container != null && container != this)
                {
                    return container;
                }
            }
            return null;
        }
    }
}
