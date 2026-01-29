using UnityEngine;
using TMPro;

namespace AnatomyLab
{
    public class AnatomyUIManager : MonoBehaviour
    {
        [Header("UI Components")]
        [SerializeField]
        private GameObject infoPanel;

        [SerializeField]
        private TextMeshProUGUI nameText;

        [SerializeField]
        private TextMeshProUGUI descriptionText;

        private void Start()
        {
            // Ensure panel is hidden on start, unless we just created it and want to test it.
            // But usually hidden is better.
            CloseInfoPanel();
        }

        public void SetupReferences(GameObject panel, TextMeshProUGUI name, TextMeshProUGUI desc)
        {
            infoPanel = panel;
            nameText = name;
            descriptionText = desc;
        }

        public void UpdateInfoPanel(string name, string description)
        {
            if (infoPanel != null)
            {
                infoPanel.SetActive(true);
            }

            if (nameText != null)
            {
                nameText.text = name;
            }

            if (descriptionText != null)
            {
                descriptionText.text = description;
            }
        }

        public void CloseInfoPanel()
        {
            if (infoPanel != null)
            {
                infoPanel.SetActive(false);
            }
        }
    }
}
