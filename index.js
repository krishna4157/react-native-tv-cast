import React from "react";
import { Text, View, Animated } from "react-native";
class TypingEffectText extends React.Component {
    animatedValues = [];
    
    constructor(props) {
        super(props);
        console.log(this.props);
        this.animatedValues = this.props.children.split('');
        this.animatedValues.forEach(( _,i)=> {
            this.animatedValues[i] = new Animated.Value(0);
        });
        this.state = {
            indexOf: 0
        }
    }

    componentDidMount(){
        this.animated();
    }



    animated  =  (toValue = 1) => {
        const animations = this.animatedValues.map((value, index) => {
            this.setState({
                indexOf : index
            });
            return Animated.timing(this.animatedValues[index],{
                toValue : toValue,
                duration :20,
                useNativeDriver: true
            });
        });

        Animated.stagger(200,animations ).start();
    }



    render() {
        return (
                <Animated.View style={{flexDirection:'row',flexWrap:'wrap'}}>
                {this.props.children.split('').map((value, index) => {
                    return (<Animated.Text key={index} style={{ textAlign: 'center', fontSize: 20 ,opacity: this.animatedValues[index], ...this.props.style}}>
                        {value}
                    </Animated.Text>);
                })
                }
                </Animated.View >
        )
    }
}

export default TypingEffectText;