using UnityEngine;
using UnityEngine.XR.Interaction.Toolkit;
using UnityEngine.XR.Interaction.Toolkit.Interactors;

namespace AnatomyLab
{
    [RequireComponent(typeof(UnityEngine.XR.Interaction.Toolkit.Interactables.XRBaseInteractable))]
    public class AnatomyObjectController : MonoBehaviour
    {
        [Header("Anatomy Data")]
        [SerializeField]
        private string anatomyName = "Anatomy Part";

        [TextArea]
        [SerializeField]
        private string description = "Description of the anatomy part.";

        private UnityEngine.XR.Interaction.Toolkit.Interactables.XRBaseInteractable interactable;

        private void Awake()
        {
            interactable = GetComponent<UnityEngine.XR.Interaction.Toolkit.Interactables.XRBaseInteractable>();
        }

        private void OnEnable()
        {
            if (interactable != null)
            {
                interactable.selectEntered.AddListener(OnSelect);
            }
        }

        private void OnDisable()
        {
            if (interactable != null)
            {
                interactable.selectEntered.RemoveListener(OnSelect);
            }
        }

        private void OnSelect(SelectEnterEventArgs args)
        {
            // Only show info if the manager exists
            if (AnatomyLabManager.Instance != null)
            {
                AnatomyLabManager.Instance.ShowInfo(anatomyName, description);
            }
        }
    }
}
