import AppKit
let size = 1024
let image = NSImage(size: NSSize(width: size, height: size))
image.lockFocus()
let rect = NSRect(x: 0, y: 0, width: size, height: size)
let bg = NSBezierPath(roundedRect: rect, xRadius: 235, yRadius: 235)
NSColor(calibratedRed: 0.08, green: 0.29, blue: 0.70, alpha: 1).setFill()
bg.fill()
let shine = NSBezierPath(ovalIn: NSRect(x: 142, y: 145, width: 740, height: 740))
NSColor(calibratedRed: 0.24, green: 0.60, blue: 0.98, alpha: 1).setFill()
shine.fill()
func stroke(_ path: NSBezierPath, width: CGFloat, color: NSColor) {
    path.lineWidth = width
    path.lineCapStyle = .round
    color.setStroke()
    path.stroke()
}
let white = NSColor.white
let meridian = NSBezierPath()
meridian.move(to: NSPoint(x: 512,y: 178))
meridian.curve(to: NSPoint(x: 512,y: 846), controlPoint1: NSPoint(x: 275,y: 405),controlPoint2:NSPoint(x:275,y:620))
meridian.curve(to: NSPoint(x: 512,y: 178), controlPoint1: NSPoint(x:749,y:620),controlPoint2:NSPoint(x:749,y:405))
stroke(meridian,width:21,color:white.withAlphaComponent(0.75))
for y in [380.0, 512.0, 644.0] {
    let p = NSBezierPath()
    p.move(to: NSPoint(x: 180, y: y))
    p.curve(to: NSPoint(x: 844,y:y), controlPoint1: NSPoint(x:340,y:y + 60),controlPoint2:NSPoint(x:680,y:y + 60))
    stroke(p,width:17,color:white.withAlphaComponent(0.63))
}
let bar = NSBezierPath(roundedRect: NSRect(x: 180,y: 410,width: 664,height: 207),xRadius:92,yRadius:92)
NSColor(calibratedRed: 0.06, green: 0.28, blue: 0.68, alpha: 1).setFill()
bar.fill()
let heights:[CGFloat] = [68, 135, 189, 135, 68]
for (i,h) in heights.enumerated() {
    let x=280+CGFloat(i)*114
    let shape=NSBezierPath(roundedRect:NSRect(x:x,y:512-h/2,width:39,height:h),xRadius:18,yRadius:18)
    white.setFill(); shape.fill()
}
let sparkle = NSBezierPath()
sparkle.move(to:NSPoint(x:825,y:864))
sparkle.line(to:NSPoint(x:850,y:925))
sparkle.line(to:NSPoint(x:875,y:864))
sparkle.line(to:NSPoint(x:934,y:842))
sparkle.line(to:NSPoint(x:875,y:819))
sparkle.line(to:NSPoint(x:850,y:758))
sparkle.line(to:NSPoint(x:825,y:819))
sparkle.line(to:NSPoint(x:765,y:842))
sparkle.close()
NSColor(calibratedRed: 1, green: 0.69, blue: 0.35, alpha: 1).setFill()
sparkle.fill()
image.unlockFocus()
guard let tiff=image.tiffRepresentation, let bitmap=NSBitmapImageRep(data:tiff),
      let png=bitmap.representation(using:.png,properties:[:]) else { fatalError("PNG encoding failed") }
try png.write(to: URL(fileURLWithPath: "ios/WorldVoiceTranslator/Assets.xcassets/AppIcon.appiconset/AppIcon.png"))
print("Created premium 1024x1024 launcher icon")
