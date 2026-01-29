using UnityEngine;

namespace ChemistryLab
{
    /// <summary>
    /// Hides the default Mobile AR Template UI elements at startup
    /// </summary>
    public class HideTemplateUI : MonoBehaviour
    {
        [Header("Template UI Objects to Hide")]
        [SerializeField] private string[] objectNamesToHide = new string[]
        {
            "UI",
            "Create Button",
            "Delete Button", 
            "Options Button",
            "Options Modal",
            "Coaching UI",
            "Greeting Prompt",
            "DebugMenu",
            "Object Menu Animator"
        };

        private void Awake()
        {
            HideTemplateUIObjects();
            DisableTemplateObjectPlacement();
        }

        private void HideTemplateUIObjects()
        {
            foreach (string objName in objectNamesToHide)
            {
                GameObject obj = GameObject.Find(objName);
                if (obj != null)
                {
                    obj.SetActive(false);
                    Debug.Log($"[ChemistryLab] Hidden template UI: {objName}");
                }
            }

            // Also find UI by looking for Canvas with specific child names
            Canvas[] allCanvases = FindObjectsByType<Canvas>(FindObjectsSortMode.None);
            foreach (var canvas in allCanvases)
            {
                // Skip our own UI
                if (canvas.gameObject.name.Contains("ChemistryLab")) continue;

                // Check if this is the template UI canvas
                Transform createBtn = canvas.transform.Find("Create Button");
                Transform deleteBtn = canvas.transform.Find("Delete Button");
                
                if (createBtn != null || deleteBtn != null)
                {
                    canvas.gameObject.SetActive(false);
                    Debug.Log($"[ChemistryLab] Hidden template canvas: {canvas.gameObject.name}");
                }
            }
        }

        private void DisableTemplateObjectPlacement()
        {
            // Disable any object spawner/placer components from the template
            // These are the components that create cubes when you tap
            
            // Find and disable "ARInteractorSpawnTrigger" or similar
            var allMonoBehaviours = FindObjectsByType<MonoBehaviour>(FindObjectsSortMode.None);
            foreach (var mb in allMonoBehaviours)
            {
                string typeName = mb.GetType().Name;
                
                // Disable template-specific object creation scripts
                if (typeName.Contains("Spawn") || 
                    typeName.Contains("ObjectCreator") ||
                    typeName.Contains("ARInteractor") ||
                    typeName.Contains("PlaceObject") ||
                    typeName.Contains("ObjectPlacer") ||
                    typeName.Contains("CreateObject"))
                {
                    // Don't disable our own scripts
                    if (mb.GetType().Namespace != null && 
                        mb.GetType().Namespace.Contains("ChemistryLab")) continue;
                    
                    mb.enabled = false;
                    Debug.Log($"[ChemistryLab] Disabled template script: {typeName} on {mb.gameObject.name}");
                }
            }

            // Find and disable XR Interactable components that might be creating objects
            // Look for GameObjects named like "Spawnable" or containing object prefabs
            string[] objectsToDisable = new string[]
            {
                "Object Spawner",
                "ARObjectSpawner", 
                "ObjectCreator",
                "SpawnManager",
                "InteractionManager"
            };

            foreach (string objName in objectsToDisable)
            {
                GameObject obj = GameObject.Find(objName);
                if (obj != null)
                {
                    obj.SetActive(false);
                    Debug.Log($"[ChemistryLab] Disabled object spawner: {objName}");
                }
            }

            // Disable any "Interactor" on XR Origin that might trigger object creation
            GameObject xrOrigin = GameObject.Find("XR Origin (AR Rig)");
            if (xrOrigin != null)
            {
                // Find interactors that have spawn triggers
                var interactors = xrOrigin.GetComponentsInChildren<MonoBehaviour>(true);
                foreach (var interactor in interactors)
                {
                    string typeName = interactor.GetType().Name;
                    if (typeName.Contains("Interactor") && typeName.Contains("Spawn"))
                    {
                        interactor.enabled = false;
                        Debug.Log($"[ChemistryLab] Disabled: {typeName}");
                    }
                }
            }
        }
    }
}
