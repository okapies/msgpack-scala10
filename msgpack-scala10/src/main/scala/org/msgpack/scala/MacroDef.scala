package org.msgpack.scala

import language.experimental.macros
import reflect.macros.Context
import org.msgpack.io.Output
import org.msgpack.packer.Packer
import org.msgpack.unpacker.Unpacker

/**
 * 
 * User: takeshita
 * DateTime: 13/02/01 20:22
 */
object MacroDef {

  def serializeSampleClass(obj : SampleClass)(implicit s : Packer) = macro _serializeSampleClass

  def _serializeSampleClass(c:Context)( obj : c.Expr[SampleClass])(s : c.Expr[Packer]) : c.Expr[Unit] = {
    import c.universe._
    val writeId = Apply( Select(s.tree,newTermName("writeLong")), List(Select(obj.tree, newTermName("id"))))
    val writeName = Apply(Select(s.tree,newTermName("writeString")), List(Select(obj.tree, newTermName("name"))))

    c.Expr[Unit](Block(writeId,writeName))
  }

  def deserializeSampleClass()(implicit s : Unpacker) : SampleClass = macro _deserializeSampleClass

  def _deserializeSampleClass(c:Context)()(s : c.Expr[Unpacker]) : c.Expr[SampleClass] = {

    import c.universe._

    val v = reify{
      val id = s.splice.readLong()
      val name = s.splice.readString()
      new SampleClass(id,name)
    }
    v


  }

  def serialize[T](obj:T)(implicit s : Packer) : Unit = macro _serialize[T]

  def _serialize[T](c:Context)( obj : c.Expr[T])(s : c.Expr[Packer]) : c.Expr[Unit] = {

    import c.universe._
    println("Start compile")
    val t = obj.actualType
    val vals = t.members.collect({
      case t : TermSymbol if t.isVal => t
    })


    val trees = vals.map(v => {
      v match{
        case term : TermSymbol => {
          term.getter.asMethod.returnType match{
            case t if t =:= typeOf[String] => {
              Apply(Select(s.tree,newTermName("writeString")), List(Select(obj.tree,term.getter.name)))
            }
            case definitions.IntTpe => {
              Apply(Select(s.tree,newTermName("writeInt")), List(Select(obj.tree,term.getter.name)))
            }
            case definitions.LongTpe => {
              Apply(Select(s.tree,newTermName("writeLong")), List(Select(obj.tree,term.getter.name)))
            }
          }
        }
      }
    }).toList

    println( showRaw(trees))
    c.Expr[Unit](Block(trees.reverse :_*))
  }



}
