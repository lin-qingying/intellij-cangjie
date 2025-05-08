func pattern(): Unit {

    if (let Some(a/*<# :Int64 #>*/)   <- Some(1)) {
    }

    while (let a/*<# :Int64 #>*/  <- Some(1)) {
    }

    for (ab/*<# :Int64 #>*/ in 1..22) {
    }

    match (A(1)) {
        case C(a/*<# :String #>*/, b/*<# :Int64 #>*/) => 1
        case a/*<# :Int64 #>*/ => 1
        case b:Int => 12
        case _ => throw Exception("Unreachable code")
    }


}
