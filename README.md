# VirtuGlow

## Sources and AI usage statement

This project builds upon an example app demonstrating DeepAR SDK usage for Android development. The following functionality was taken:

- Preview of fun face filters.
- Take screenshot.
- Record video.
- Front and back camera.
- Source code demonstrates how to integrate DeepAR for Android.

For more info on DeepAR for Android see: https://docs.deepar.ai/deepar-sdk/platforms/android/overview

Claude Code, Claude, and Gemini were used throughout development to assist mainly with:

- understanding DeepAR sample code
- cleaning up not used sample project code
- suggestions to improve DeepAR performance
- creating repetitive UI elements
- debugging when the issue persisted
- writing detailed comments
- explaining documentation
- recommending best practices for app structure




## Requirements

1. essential

- client user account creation
- client user log in
- storing client personal and makeover information in a database
- encrypted strings for password - hashed and salted
- successful integration of DeepAR SDK
- permissions for the phone’s camera and microphone usage
- client being able to see makeup on face
- recording video with the makeover
- taking picture with makeover

2. main

- agreement Terms of Use and Privacy Policy
- shop for users
- image preview of the available makeovers in the shop
- video preview of the available makeovers in the shop
- satisfaction scores and average satisfaction scores 
- reviews for MUA for common free makeovers
- tags for filtering + search function (by tag)
- ranking of makeovers
- MUA personal look catalogue
- window for uploading a new makeover

3. optional

- modify user info once account is created 
- profile pictures and changing them
- forgot password
- reviews for MUA for custom filters
- delete account and personal information
- window for requesting a new makeover
- window for MUA to modify makeovers 
- encrypt deepAR files

## Design patterns                                                                                             
                                                                                                                    
1. Facade / Manager pattern                                                                                          
                                                                                                                    
DatabaseManager, DeepARManager, and CameraManager each hide a complex subsystem behind a simple interface. Activities never import OkHttp, DeepAR SDK classes, or CameraX — they only call the manager.                                                          
                                                            
2. Callback / Observer pattern

Every async operation exposes a typed callback interface: LoginCallback, SimpleCallback, APICallback, FileCallback, DeepARManager.Listener, FilterDialogFragment.OnFiltersApplied.                                                                 
                                                            
3. Template Method pattern

DrawerMenu defines a step (startDrawer()) that every subclass must call in onCreate(). The base class owns the drawer wiring logic; subclasses just trigger it at the right moment.                                                         
                                                            
4. Adapter pattern (RecyclerView)

MakeoverAdapter and ShopAdapter both implement the standard Android Adapter + ViewHolder pattern — adapting a plain List<T> into something RecyclerView can render, with view recycling handled by ViewHolder.
                                                                                                                                                                     
5. Double-buffer pattern *belongs to the sample DeepAR project

CameraManager alternates between two ByteBuffers each frame. While DeepAR processes buffer[0], CameraX writes the next frame into buffer[1] and vice versa — a classic producer/consumer double-buffer to avoid frame drops.

