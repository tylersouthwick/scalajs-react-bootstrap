package com.acework.js.components.bootstrap

import com.acework.js.utils.{Mappable, Mergeable}
import japgolly.scalajs.react.Addons.ReactCloneWithProps
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom.raw.HTMLElement

import scala.collection.mutable.ArrayBuffer
import scala.scalajs.js.{Any => JAny, UndefOr, undefined}

/**
 * Created by weiyin on 09/03/15.
 */

object Panel extends BootstrapComponent {
  override type P = Props
  override type S = CollapsableState
  override type B = Backend
  override type N = TopNode

  override def defaultProps = Props()

  case class Props(collapsable: UndefOr[Boolean] = undefined,
                   defaultExpanded: Boolean = false,
                   expanded: UndefOr[Boolean] = undefined,
                   header: UndefOr[ReactNode] = undefined,
                   footer: UndefOr[String] = undefined,
                   eventKey: UndefOr[String] = undefined,
                   onSelect: UndefOr[(UndefOr[String]) => Unit] = undefined,
                   id: UndefOr[String] = undefined,
                   bsClass: UndefOr[Classes.Value] = Classes.panel,
                   bsStyle: UndefOr[Styles.Value] = Styles.default,
                   bsSize: UndefOr[Sizes.Value] = undefined,
                   addClasses: String = "") extends BsProps with CollapsableProps with MergeableProps[Props] {

    def merge(t: Map[String, Any]): Props = implicitly[Mergeable[Props]].merge(this, t)

    def asMap: Map[String, Any] = implicitly[Mappable[Props]].toMap(this)
  }

  class Backend(val scope: BackendScope[Props, CollapsableState]) extends CollapsableMixin[Props] {
    var _isChanging: Boolean = _

    def handleSelect(e: ReactEvent) = {
      if (scope.props.onSelect.isDefined) {
        _isChanging = true
        scope.props.onSelect.get(scope.props.eventKey.get)
        _isChanging = false
      }
      e.preventDefault()
      scope.modState(s => s.copy(isExpended = !s.isExpended))
    }

    def getCollapsableDimensionValue: Int = {
      getCollapsableDOMNode match {
        case Some(panel) =>
          panel.scrollHeight

        case None =>
          0
      }
    }

    def getCollapsableDOMNode: Option[TopNode] = {
      if (scope.isMounted() && scope.refs != null && scope.refs("panel") != null)
        Some(scope.refs("panel").asInstanceOf[TopNode])
      else
        None
    }
  }

  override val component = ReactComponentB[Props]("Panel")
    .initialStateP(P => CollapsableState(collapsing = false, isExpended = P.defaultExpanded))
    .backend(new Backend(_))
    .render { (P, C, S, B) =>
    val panelRef = Ref[HTMLElement]("panel")

    def renderCollapsableTitle(heading: String) = {
      <.h4(^.className := "panel-title")(renderAnchor(heading))
    }
    def renderHeading(): TagMod = {
      // TODO when header is a Node

      if (P.header.isDefined) {
        if (React.isValidElement(P.header.get)) {
          val header = if (P.collapsable.getOrElse(false)) {
            ReactCloneWithProps(P.header.get, Map[String, JAny](
              "className" -> "panel-title" // ,
              // FIXME "children" -> renderAnchor(P.header.props.children)
            ))
          }
          else {
            ReactCloneWithProps(P.header.get, Map[String, JAny]("className" -> "panel-title"))
          }
          <.div(^.className := "panel-heading")(header)
        }
        else {
          val header: ReactNode = if (P.collapsable.getOrElse(false))
            renderCollapsableTitle(P.header.get.asInstanceOf[String])
          else
            P.header.get
          <.div(^.className := "panel-heading")(header)
        }
      }
      else
        EmptyTag
    }

    def renderAnchor(header: String) = {
      // FIXME check isExpanded
      <.a(^.href := s"#${P.id.getOrElse("")}", ^.className := "", ^.onClick ==> B.handleSelect)(header)
    }

    def renderFooter() = {
      if (P.footer.isDefined)
        <.div(^.className := "panel-footer")(P.footer.get)
      else
        EmptyTag
    }


    def renderBody(): ReactNode = {
      val bodyElements = new ArrayBuffer[ReactNode]

      def shouldRenderFill(c: ReactNode) =
        React.isValidElement(c) && false // TODO: c.props.fill != null

      def getProps: Map[String, JAny] = Map("key" -> bodyElements.length)

      def addPanelChild(c: ReactNode) = {
        val node: ReactNode = ReactCloneWithProps(c, getProps)
        bodyElements += node
      }

      def addPanelBody(c: ReactNode*) = {
        bodyElements += <.div(^.className := "panel-body")(c)
      }

      val numChildren = React.Children.count(C)

      if (numChildren == 0) {
        if (shouldRenderFill(C))
          addPanelChild(C)
        else
          addPanelBody(C)
      }
      else {
        val panelBodyChildren = new ArrayBuffer[ReactNode]

        def maybeRenderPanelBody() = {
          if (panelBodyChildren.length > 0) {
            addPanelBody(panelBodyChildren)
          }
        }

        C.forEach { child =>
          if (shouldRenderFill(child)) {
            maybeRenderPanelBody()

            // separately add the filled element
            addPanelChild(child)
          }
          else
            panelBodyChildren += child
        }
        maybeRenderPanelBody()
      }
      bodyElements
    }

    def renderCollapsableBody(): ReactNode = {
      <.div(^.classSetM(B.getCollapsableClassSet("panel-collapse")), ^.id := P.id, ^.ref := panelRef)(renderBody())
    }

    val classes = P.bsClassSet + ("panel" -> true)

    val hasId = !P.collapsable.getOrElse(false) && P.id.isDefined
    <.div(^.classSet1M(P.addClasses, classes), hasId ?= (^.id := P.id.get))(
      renderHeading(),
      if (P.collapsable.getOrElse(false)) renderCollapsableBody() else renderBody(),
      renderFooter()
    )
  }
    .shouldComponentUpdate(($, _, _) => !$.backend._isChanging)
    .build

}
