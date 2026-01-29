using UnityEngine;
using UnityEngine.InputSystem;
using UnityEngine.InputSystem.EnhancedTouch;
using Touch = UnityEngine.InputSystem.EnhancedTouch.Touch;

namespace ChemistryLab.Interaction
{
    using Containers;
    using UI;

    /// <summary>
    /// Handles touch/tap interaction with lab equipment on mobile AR
    /// </summary>
    public class LabTouchInteraction : MonoBehaviour
    {
        [Header("References")]
        [SerializeField] private ChemistryLabUIManager uiManager;
        [SerializeField] private Camera arCamera;

        [Header("Settings")]
        [SerializeField] private float doubleTapTime = 0.3f;
        [SerializeField] private float dragThreshold = 10f;
        [SerializeField] private LayerMask interactableLayers = -1;

        private ChemicalContainer _selectedContainer;
        private ChemicalContainer _heldContainer;
        private GameObject _heldObject; // For any draggable object (including burner)
        private GameObject _selectedObject; // Track the currently selected object for deletion
        private float _lastTapTime;
        private Vector2 _lastTapPosition;
        private Vector2 _dragStartPosition;
        private bool _isDragging;
        private float _holdTime;
        private bool _isHolding;

        /// <summary>
        /// The currently selected container (for use by other systems like heating UI)
        /// </summary>
        public ChemicalContainer SelectedContainer => _selectedContainer;

        private void OnEnable()
        {
            EnhancedTouchSupport.Enable();
        }

        private void OnDisable()
        {
            EnhancedTouchSupport.Disable();
        }

        private void Start()
        {
            if (arCamera == null)
                arCamera = Camera.main;
        }

        private void Update()
        {
            HandleTouchInput();
            HandleDragInput();
        }

        private void HandleTouchInput()
        {
            Vector2 inputPos = Vector2.zero;
            bool isTapStart = false;
            bool isTapEnd = false;
            bool isHeld = false;

            // Touch input (new Input System)
            if (Touch.activeTouches.Count > 0)
            {
                var touch = Touch.activeTouches[0];
                inputPos = touch.screenPosition;
                isTapStart = touch.phase == UnityEngine.InputSystem.TouchPhase.Began;
                isTapEnd = touch.phase == UnityEngine.InputSystem.TouchPhase.Ended;
                isHeld = touch.phase == UnityEngine.InputSystem.TouchPhase.Moved || 
                         touch.phase == UnityEngine.InputSystem.TouchPhase.Stationary;
            }
            // Mouse input (for editor testing)
            else if (Mouse.current != null)
            {
                inputPos = Mouse.current.position.ReadValue();
                isTapStart = Mouse.current.leftButton.wasPressedThisFrame;
                isTapEnd = Mouse.current.leftButton.wasReleasedThisFrame;
                isHeld = Mouse.current.leftButton.isPressed;
            }

            if (isTapStart)
            {
                _dragStartPosition = inputPos;
                _holdTime = 0;
                _isHolding = true;
                TryPickupObject(inputPos);
            }
            
            if (isHeld && _isHolding)
            {
                _holdTime += Time.deltaTime;
                // Start dragging if moved beyond threshold
                if (!_isDragging && Vector2.Distance(inputPos, _dragStartPosition) > dragThreshold)
                {
                    _isDragging = true;
                }
            }

            if (isTapEnd)
            {
                if (!_isDragging && _holdTime < 0.3f)
                {
                    // Quick tap - just select, don't move
                    ProcessTap(inputPos);
                }
                _isHolding = false;
                _isDragging = false;
                ReleaseObject();
            }
        }

        private void TryPickupObject(Vector2 screenPos)
        {
            Ray ray = arCamera.ScreenPointToRay(screenPos);
            RaycastHit hit;

            if (Physics.Raycast(ray, out hit, 10f, interactableLayers))
            {
                // Check for container
                var container = hit.collider.GetComponent<ChemicalContainer>();
                if (container != null)
                {
                    _heldContainer = container;
                    _heldObject = container.gameObject;
                    SelectContainer(container);
                    Debug.Log($"Picked up: {container.ContainerName}");
                    return;
                }

                // Check for other physics objects
                var rb = hit.collider.GetComponent<Rigidbody>();
                if (rb != null)
                {
                    _heldObject = hit.collider.gameObject;
                    _selectedObject = _heldObject; // Select it
                    Debug.Log($"Picked up: {_heldObject.name}");
                    return;
                }
            }
        }

        private void ProcessTap(Vector2 screenPos)
        {
            // Check for double-tap
            bool isDoubleTap = (Time.time - _lastTapTime < doubleTapTime) &&
                               (Vector2.Distance(screenPos, _lastTapPosition) < 50f);
            
            _lastTapTime = Time.time;
            _lastTapPosition = screenPos;

            // Raycast
            Ray ray = arCamera.ScreenPointToRay(screenPos);
            RaycastHit hit;

            if (Physics.Raycast(ray, out hit, 10f, interactableLayers))
            {
                // Check for bunsen burner - toggle on tap
                var burner = hit.collider.GetComponent<BunsenBurner>();
                if (burner != null)
                {
                    burner.Toggle();
                    return;
                }
            }
        }

        private void HandleDragInput()
        {
            if (_heldObject == null || !_isDragging) return;

            Vector2 inputPos = Vector2.zero;
            bool isActive = false;

            // Touch (new Input System)
            if (Touch.activeTouches.Count > 0)
            {
                var touch = Touch.activeTouches[0];
                inputPos = touch.screenPosition;
                isActive = touch.phase == UnityEngine.InputSystem.TouchPhase.Moved || 
                           touch.phase == UnityEngine.InputSystem.TouchPhase.Stationary;
            }
            // Mouse (new Input System)
            else if (Mouse.current != null && Mouse.current.leftButton.isPressed)
            {
                inputPos = Mouse.current.position.ReadValue();
                isActive = true;
            }

            if (isActive)
            {
                MoveHeldObject(inputPos);
            }
        }

        private void SelectContainer(ChemicalContainer container)
        {
            _selectedContainer = container;
            _selectedObject = container.gameObject; // Also track as selected object
            
            if (uiManager != null)
            {
                uiManager.SelectContainer(container);
            }

            Debug.Log($"Selected: {container.ContainerName}");
        }

        private void MoveHeldObject(Vector2 screenPos)
        {
            if (_heldObject == null) return;

            // Keep the original rotation - don't let dragging rotate the object
            Quaternion originalRotation = _heldObject.transform.rotation;
            float originalY = _heldObject.transform.position.y; // Keep same height

            // Project screen position to world on XZ plane
            Ray ray = arCamera.ScreenPointToRay(screenPos);
            
            // Raycast to a horizontal plane at the object's current height
            Plane horizontalPlane = new Plane(Vector3.up, new Vector3(0, originalY, 0));
            
            if (horizontalPlane.Raycast(ray, out float distance))
            {
                Vector3 targetPos = ray.GetPoint(distance);
                targetPos.y = originalY; // Ensure Y stays constant
                
                _heldObject.transform.position = Vector3.Lerp(
                    _heldObject.transform.position, 
                    targetPos, 
                    Time.deltaTime * 15f
                );
            }
            
            // IMPORTANT: Restore upright rotation - keep object standing upright
            _heldObject.transform.rotation = Quaternion.Euler(0, originalRotation.eulerAngles.y, 0);
        }

        private void ReleaseObject()
        {
            if (_heldObject == null) return;

            // Keep kinematic true so it doesn't fall
            var rb = _heldObject.GetComponent<Rigidbody>();
            if (rb != null)
            {
                rb.isKinematic = true;
                rb.useGravity = false;
            }

            // Check if pouring into another container
            if (_heldContainer != null)
            {
                CheckPourTarget();
            }

            Debug.Log($"Released: {_heldObject.name}");
            _heldContainer = null;
            _heldObject = null;
        }

        private void CheckPourTarget()
        {
            if (_heldContainer == null) return;

            // Find nearby containers
            Collider[] nearby = Physics.OverlapSphere(_heldContainer.transform.position, 0.15f);
            foreach (var col in nearby)
            {
                var otherContainer = col.GetComponent<ChemicalContainer>();
                if (otherContainer != null && otherContainer != _heldContainer)
                {
                    // Check if tilted to pour
                    if (_heldContainer.ShouldPour())
                    {
                        _heldContainer.PourInto(otherContainer, 10f);
                        Debug.Log($"Poured from {_heldContainer.ContainerName} into {otherContainer.ContainerName}");

                        // Play pouring sound
                        if (Effects.RealisticEffects.Instance != null && _heldContainer.GetChemicals().Count > 0)
                        {
                            Effects.RealisticEffects.Instance.PlayPouringSound(_heldContainer.transform.position, true);
                            StartCoroutine(StopPouringSoundAfterDelay(1.0f));
                        }
                    }
                    break;
                }
            }
        }



        private System.Collections.IEnumerator StopPouringSoundAfterDelay(float delay)
        {
            yield return new WaitForSeconds(delay);
            if (Effects.RealisticEffects.Instance != null)
            {
                Effects.RealisticEffects.Instance.PlayPouringSound(Vector3.zero, false);
            }
        }


        /// <summary>
        /// Deletes the currently selected object
        /// </summary>
        public void DeleteSelectedObject()
        {
            if (_selectedObject != null)
            {
                // If it's the held object, release it first
                if (_heldObject == _selectedObject)
                {
                    ReleaseObject();
                }

                // If it's the selected container, clear that ref
                if (_selectedContainer != null && _selectedContainer.gameObject == _selectedObject)
                {
                    _selectedContainer = null;
                    if (uiManager != null)
                    {
                        uiManager.SelectContainer(null);
                    }
                }

                Destroy(_selectedObject);
                _selectedObject = null;
                Debug.Log("Deleted selected object");
            }
        }

        public ChemicalContainer HeldContainer => _heldContainer; // Keep this property!
    }
}
