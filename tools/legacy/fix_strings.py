import os
import xml.etree.ElementTree as ET

res_dir = 'app/src/main/res'
base_strings_file = os.path.join(res_dir, 'values', 'strings.xml')

# Keys to add
keys_to_add = [
    "app_exit_confirm_title",
    "app_exit_confirm_message",
    "app_exit_confirm_exit",
    "app_exit_confirm_cancel",
    "settings_require_exit_confirmation_label",
    "settings_require_exit_confirmation_description",
    "walkthrough_page1_title",
    "walkthrough_page1_desc",
    "walkthrough_page2_title",
    "walkthrough_page2_desc",
    "walkthrough_page3_title",
    "walkthrough_page3_desc",
    "walkthrough_back",
    "walkthrough_next",
    "walkthrough_finish",
    "settings_show_walkthrough_button"
]

# Get the values from base strings.xml (which we just wrote to)
# Wait, some walkthrough strings are in strings_walkthrough.xml. Let's just hardcode the XML elements to append.

xml_snippet = """
    <!-- Exit Confirmation -->
    <string name="app_exit_confirm_title">Exit App</string>
    <string name="app_exit_confirm_message">Are you sure you want to exit?</string>
    <string name="app_exit_confirm_exit">Exit</string>
    <string name="app_exit_confirm_cancel">Cancel</string>
    <string name="settings_require_exit_confirmation_label">Require Exit Confirmation</string>
    <string name="settings_require_exit_confirmation_description">Show a confirmation popup when pressing the back button to exit the app.</string>

    <!-- Walkthrough -->
    <string name="walkthrough_page1_title">Welcome to QuranicWords</string>
    <string name="walkthrough_page1_desc">Learn Quranic vocabulary through interactive exercises. Follow our guided learning path to master the most frequent words in the Quran.</string>
    <string name="walkthrough_page2_title">Interactive Exercises</string>
    <string name="walkthrough_page2_desc">Practice with quizzes, matching games, and reviews. Build your daily streak and earn points as you progress through the lessons.</string>
    <string name="walkthrough_page3_title">Track Your Progress</string>
    <string name="walkthrough_page3_desc">View your achievements, daily goals, and see what percentage of the Quran you can understand. Stay motivated and consistent!</string>
    <string name="walkthrough_back">Back</string>
    <string name="walkthrough_next">Next</string>
    <string name="walkthrough_finish">Get Started</string>
    <string name="settings_show_walkthrough_button">Show Walkthrough Guide</string>
"""

for d in os.listdir(res_dir):
    if d.startswith('values-') and d != 'values-night':
        target_file = os.path.join(res_dir, d, 'strings.xml')
        if os.path.exists(target_file):
            with open(target_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            if "app_exit_confirm_title" not in content:
                # Insert before </resources>
                content = content.replace("</resources>", xml_snippet + "</resources>")
                with open(target_file, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f"Updated {target_file}")

