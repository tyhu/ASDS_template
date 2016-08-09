from Tkinter import *
import os


def highlight_text(text_widget, text, tag):
    # Search for the text to highlight
    search_term = text
    highlight_idx = text_widget.search(search_term, '1.0', END) #index is defines as <line>.<char_pos>
    highlight_pos = '{}+{}c'.format(highlight_idx,len(search_term))
    
    # Finally highlight the text
    text_widget.tag_add(tag, highlight_idx, highlight_pos)    

def task():
    highlight_text(text_widget, 'Paul Racicot', 'yellow')
    highlight_text(text_widget, 'North America', 'blue')
    highlight_text(text_widget, 'Paul Racicot', 'yellow')
    highlight_text(text_widget, '$100,000', 'red')
    
    s = raw_input('input something:')
    text_widget.delete("1.0", END)
    text_widget.insert(END, s)
    


    
# This is the demo text, which is in 3 lines
email_text = '''Nick, 
I spoke with Paul Racicot, head of trading for  EBS, North America this morning.   He said that he is happy to send the $100,000 for your program from his budget.   I have forwarded to him the draft letter to accompany the funds and will try to follow up to make sure that the money is sent promptly. 
--Stinson '''


# Main canvas for Tkinter
root = Tk()

text_widget = Text(root, height=10, width=50)
text_widget.pack() # refresh on canvas
text_widget.insert(END, email_text) #Display the text in widget


# Configure the possible highlight tags
# param1: custom name
# param2: actual color, it can go in HTML format
text_widget.tag_config('red', background='red', foreground='white')
text_widget.tag_config('yellow', background='yellow')
text_widget.tag_config('blue', background='blue', foreground='white')
text_widget.tag_config('green', background='green')

root.after(2000, task)
root.mainloop()

#os.system('pause')

